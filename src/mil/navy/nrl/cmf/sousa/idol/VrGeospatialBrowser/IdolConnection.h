#ifndef IDOLFACADE_H
#define IDOLFACADE_H

// facade for receiving data/commands from the federation of Idol agents

#include <iostream>
#include <sstream>
#include <string>
#include <map>
#include <osg/Group>
#include "jglobe3.h"
#include "sysVipc.h"
#include "Utils.h"

using std::string;
using std::vector;
using std::map;
using std::cout;
using std::cerr;
using std::endl;

class IdolConnection {
public:
    IdolConnection(osgProducer::Viewer* viewer, osg::Group* group) :
		viewer_(viewer), 
		layerGroup_(group),
		lat_(0.0), lon_(0.0), elev_(0.0), time_(0.0)
	{ }
    
    ~IdolConnection()
	{ }
	
    void updateCenterOfInterest(float lat, float lon, float elev, double time)
        // update Idol's spatiotemporal center of interest query.
	// we continually ask the idol federation "what objects are near this point?"
	{
	    lat_ = lat;
	    lon_ = lon;
	    elev_ = elev;
	    time_ = time;
	    exportQuery();
	}

	bool processIdolCommands(float* lat, float* lon, float* elev, double* time)
	// Receive and execute commands from idol that update our view of
	// the world.  Return true if idol changed any of our shared
	// state: location and time.
	{
		bool answer = false;
	    // theoretically there only needs to be one queue, but this
	    // idol federation was originally built for a more complex
	    // viewer app that had multiple queues across multiple threads.
	    answer |= processCommands(ipc_.ext2dbase, "ipc_.ext2dbase");
	    answer |= processCommands(ipc_.ext2compute, "ipc_.ext2compute");

		*lat = lat_;
		*lon = lon_;
		*elev = elev_;
		*time = time_;

		return answer;
	}


protected:
    osgProducer::Viewer* viewer_;
    osg::ref_ptr<osg::Group> layerGroup_;
	// group node for attachment of layer geometry
    
    map<string, DataLayer*> layers_;
	// collection of current layers
    
    float lat_;
    float lon_;
    float elev_;
    double time_;
	// current query, our center of interest 

    struct SHMData {
        float lat;
        float lon;
        float elev;
        char time[17]; // yyyyMMddhhmmssSSS
    }; // shmem data structure for "spatiotemporal center of interest" export.

    struct IPCinfo {
	int ext2compute;    // commands from idol agents to osgidolviewer
	int ext2dbase;	    // commands from idol agents to osgidolviewer
	int compute2ext;    // commands from osgidolviewer to idol agents
	int dbase2ext;	    // commands from osgidolviewer to idol agents
	int ext;	    // SysV shared memory id for query segment
	int rsem;	    // SysV semaphore for signalling shmem update
	int wsem;	    // SysV semaphore for writing safely to shmem
	
	IPCinfo() :
	    ext2compute(sysVipc::initMSQ((key_t)jglobe3_EXT2COMPUTEK)), 
	    ext2dbase(sysVipc::initMSQ((key_t)jglobe3_EXT2DBASEK)), 
	    compute2ext(sysVipc::initMSQ((key_t)jglobe3_COMPUTE2EXTK)), 
	    dbase2ext(sysVipc::initMSQ((key_t)jglobe3_DBASE2EXTK)), 
	    ext(sysVipc::initSHM((key_t)jglobe3_SHMK, sizeof(SHMData))), 
	    rsem(sysVipc::initSEM((key_t)jglobe3_RSEMK, 1)), 
	    wsem(sysVipc::initSEM((key_t)jglobe3_WSEMK, 1))
	    { }
	~IPCinfo()
	    {
		sysVipc::releaseMSQ(ext2compute);
		sysVipc::releaseMSQ(ext2dbase);
		sysVipc::releaseMSQ(compute2ext);
		sysVipc::releaseMSQ(dbase2ext);
		sysVipc::releaseSHM(ext);
		sysVipc::releaseSEM(rsem);
		sysVipc::releaseSEM(wsem);
	    }
    } ipc_; 
	// IPC connections
    
    bool processCommands(int ipcQueue, const char* queueName)
	// read and execute all commands from the given ipc queue Return
	// true if the commands changed the state this viewer shares with
	// idol: location and time.
	{
		bool answer = false;
	    assert(ipcQueue >= 0);

	    sysVipc::IPCMessage message;
	    while (msgrcv(ipcQueue, (void*)&message, sizeof(sysVipc::IPCMessage) - sizeof(long), sysVipc::MTYPE, IPC_NOWAIT) >= 0) {
			answer = executeCommand(message.mtext);
		}

	    if (ENOMSG != errno)
		cerr << "WARNING: An error occurred while reading from " << queueName << " message queue!" << endl;

		return answer;
	}

    bool executeCommand(const char* a)
        // execute a single Idol command
	{
		bool answer = false;
	    vector<string> tokens;
	    Utils::lexQuotedTokens(a, &tokens);
	    assert(tokens.size() > 0);
	    
	    if ("create" == tokens[1]) {
			// create a new layer
			// create <layername> <bookmark>
			map<string, DataLayer*>::iterator i = layers_.find(tokens[2]);
			if (i == layers_.end()) {
				layers_[tokens[2]] = new DataLayer(tokens[2]);
				layerGroup_->addChild(layers_[tokens[2]]->layerRoot());
			}
		
	    } else if ("delete" == tokens[1]) {
			// delete a layer
			// delete <layername>
			map<string, DataLayer*>::iterator i = layers_.find(tokens[2]);
			if (i != layers_.end()) {
				layerGroup_->removeChild(i->second->layerRoot());
				delete i->second;
				layers_.erase(i);
			}

	    } else if ("modify" == tokens[1] && "addchild" == tokens[3]) {
			// add a child to a layer
			map<string, DataLayer*>::iterator i = layers_.find(tokens[2]);
			if (i != layers_.end())
				i->second->addChild(tokens);

	    } else if (("update" == tokens[1]) || ("change" == tokens[1] && "set" == tokens[3])) {
			// move a child in a layer
			map<string, DataLayer*>::iterator i = layers_.find(tokens[2]);
			if (i != layers_.end())
				i->second->moveChild(tokens);
		
	    } else if ("change" == tokens[1] && "removechild" == tokens[3]) {
			// remove a child from a layer
			map<string, DataLayer*>::iterator i = layers_.find(tokens[2]);
			if (i != layers_.end())
				i->second->removeChild(tokens);
		
	    } else if (("gotolatlonelev" == tokens[1]) && 
				   (tokens.size() == 5)) {
			float lat = Utils::asDouble(tokens[2]);
			float lon = Utils::asDouble(tokens[3]);
			float elev = Utils::asDouble(tokens[4]);

			orientViewer(lat, lon, elev);
			answer = true;

			cout << "orientViewer("
				 << lat << ", "
				 << lon << ", "
				 << elev << ");" << endl;

		} else if (("gotolatlon" == tokens[1]) &&
				   (tokens.size() == 4)) {
			float lat = Utils::asDouble(tokens[2]);
			float lon = Utils::asDouble(tokens[3]);

			orientViewer(lat, lon, elev_);
			answer = true;

			cout << "orientViewer("
				 << lat << ", "
				 << lon << ", "
				 << elev_ << ");" << endl;

		} else if (("settime" == tokens[1]) &&
				   (tokens.size() == 3)) {
			time_ = Utils::asDouble(tokens[2]);
			cout << "IDOL set time to " << time_ << endl;
			answer = true;
		} else 
			cerr << "IdolConnection::executeCommand() unknown command: " 
				 << a << endl;

		return answer;
	}
	
	// Makes the Viewer reorient itself to the new latitude,
	// longitude, and elevation.
	void orientViewer(float lat, float lon, float elev) {
		osg::Vec3 eye, center, up;

		// This code is the inverse of
		// odgidolviewer::computeCenterOfInterest().
		osg::Matrixd viewMatrix = viewer_->getViewMatrix();
		viewMatrix.getLookAt(eye, center, up);

		cout << "orientViewer: New position GEO from ControlLogic " 
			 << lat << ", " << lon << ", " << elev << endl;

		// The Viewer works in terms of <x, y, z> 
		// not <lat, lon, elev>.
		//
		// elev is in meters.
		//geo2xyz expects elevation in planetary radii, not meters
		eye = Utils::geo2xyz(lat, lon,
							 (elev / Utils::planetRadius) + 1.0);

		cout << "orientViewer: Computed XYZ position "
			 << eye << endl;

		viewMatrix.makeLookAt(eye, center, up);
		viewer_->setView(viewMatrix);
	}

    void exportQuery()
        // Export current query to shmem segment
	{
	    struct SHMData* query = (struct SHMData *)shmat(ipc_.ext, NULL, 0);
	    
	    if (query < 0) {
		cerr << "WARNING: An error occurred while attaching to the external shmem segment!" << endl;
	    } else {
		// wsem is used to protect writes to the shmem segment;
		// all processes must acquire this semaphore when attempting
		// to read from or write to shmem (here we are writing).
		if (semaphoreOp(ipc_.wsem, 0, -1, IPC_NOWAIT) < 0) {
		    cerr << "WARNING: " << (EAGAIN == errno ? "Timeout" : "Error") << " occurred while locking the external shmem area before write!" << endl;
    
		} else {
		    copyQuery(query);
	    
		    // Unlock the shmem segment so external processes can read it safely.
		    if (semaphoreOp(ipc_.wsem, 0, 1, 0) < 0) 
			cerr << "WARNING: An error occurred while unlocking the external shmem area after write!" << endl;
	    
		    // rsem is used to synchronize a single external 
		    // process with the DBASE process.  Here the value
		    // of the semaphore is set to "1" if it is currently
		    // "0".  The external process should block until it
		    // can safely subtract "1" from the value to make it
		    // "0" again.
		    int val = semctl(ipc_.rsem, 0, GETVAL);
		    if (val < 0)
			cerr << "WARNING: An error occurred while asserting the external shmem signal semaphore!" << endl;
		    else if (val == 0 && semaphoreOp(ipc_.rsem, 0, 1, 0) < 0) 
			cerr << "WARNING: An error occurred while setting the external shmem signal semaphore!" << endl;
		}
	    
		if (shmdt(query) < 0)
		    cerr << "WARNING: An error occurred while detaching from the external shmem segment!" << endl;
	    }
	}

    void copyQuery(struct SHMData *query)
        // actual query data copy routine, used in exportQuery()
	{
	    // Spatial center of interest
	    query->lat = lat_;
	    query->lon = lon_;
	    query->elev = elev_;
	
	    // Temporal center of interest
	    int year, month, day, hours, minutes, seconds, ms;
	    const char *dofw;
	    Utils::secondsToGregorianDateTime(time_, &year, &month, &day, &hours, &minutes, &seconds, &ms, &dofw);
	    sprintf(query->time, "%4.4d%2.2d%2.2d%2.2d%2.2d%2.2d%3.3d", year, month, day, hours, minutes, seconds, ms);  // yyyyMMddHHmmssSSS
	}

    int semaphoreOp(int id, int num, int op, int flg)
	// do a semop, used in exportQuery()
	{
	    struct sembuf s;
	    s.sem_num = num;
	    s.sem_op = op; 
	    s.sem_flg = flg;
	    return semop(id, &s, 1);
	}

};
#endif
