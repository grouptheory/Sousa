#include <iostream>
#include <string.h>
#include "sysVipc.h"

using namespace std;

int sysVipc::initSHM(int key, int size)
{
    int result;
    // Try to create a new SysV shmem segment
    if (((result = shmget(key, size, IPC_CREAT | S_IRWXU)) < 0) && (errno == EEXIST)) {
        // Try to attach to existing SysV shmem segment
        result = shmget(key, size, S_IRWXU);
    }
    if (result < 0) {
        cerr << "WARNING: Unable to init shmem segment!" << endl;
    }
    return result;
};

void sysVipc::releaseSHM(int id)
{
    if (shmctl(id, IPC_RMID, NULL) < 0) {
        cerr << "WARNING: An error occurred while releasing shmem segment!" << endl;
    }
};

int sysVipc::initSEM(int key, int initval)
{
    int result;
    // Try to create a new SysV semaphore
    if (((result = semget(key, 1, IPC_CREAT | S_IRWXU)) < 0) && (errno == EEXIST)) {
        // Try to attach to existing SysV semaphore
        result = semget(key, 1, S_IRWXU);
    }
    if (result < 0) {
        cerr << "WARNING: Unable to init semaphore!" << endl;
    } else {
        // Initiliaze the semaphore
        union semun {
            int val;
            struct semid_ds *buf;
            ushort *array;
        } argument;
        argument.val = initval;
        if (semctl(result, 0, SETVAL, argument) < 0) {
            cerr << "WARNING: An error occurred while "
                "initializing a semaphore's value!" << endl;
        }
    }
    return result;
};

void sysVipc::releaseSEM(int id)
{
    if (semctl(id, 0, IPC_RMID)) {
        cerr << "WARNING: An error occurred while releasing semaphore!" << endl;
    }
};

int sysVipc::initMSQ(int key)
{
    int result;
    // Try to create a new SysV message queue
    if (((result = msgget(key, IPC_CREAT | S_IRWXU)) < 0) && (errno == EEXIST)) {
        // Try to attach to existing SysV message queue
        result = msgget(key, S_IRWXU);
    }
    if (result < 0) {
        cerr << "WARNING: Unable to init message queue!" << endl;
    }
    return result;
};

void sysVipc::releaseMSQ(int id)
{
    if (msgctl(id, IPC_RMID, NULL) < 0) {
        cerr << "WARNING: An error occurred while releasing message queue!" << endl;
    }
};

int sysVipc::sendToMessageQ(const char *msg, long mtype, int msqid)
{
    if (msqid < 0) { 
        cerr << "WARNING: An attempt was made to write "
            "to an invalid message queue!" << endl;
    } else {
        // Send msg to specified SysV message queue
        struct IPCMessage sysvmsg;

        sysvmsg.mtype = mtype;
        strncpy(sysvmsg.mtext, msg, MBUF_SIZE);
        if (msgsnd(msqid, (void *)&sysvmsg,
            // XXX: if IPC_NOWAIT is to be used, the messages must be retried on EAGAIN
            sizeof(struct IPCMessage) - sizeof(long), 0 /* IPC_NOWAIT */) < 0) {
            if (EAGAIN == errno) {
                cerr << "WARNING: An overflow occurred while "
                    "writing to the external message queue!" << endl;
                return 1;
            } else {
                cerr << "WARNING: An error occurred while "
                    "writing to the external message queue!" << endl;
                return -1;
            }
        }
    }

    return 0;
};
