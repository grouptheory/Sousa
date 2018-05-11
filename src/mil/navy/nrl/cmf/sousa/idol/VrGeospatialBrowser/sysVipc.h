#ifndef SYSVIPC_H
#define SYSVIPC_H

// Unix System V interprocess communication

#include <fcntl.h>
#include <errno.h>
#include <sys/stat.h>
#include <sys/ipc.h>
#include <sys/msg.h>
#include <sys/sem.h>
#include <sys/shm.h>

namespace sysVipc
{
    int initSHM(int key, int size);
    void releaseSHM(int id);

    int initSEM(int key, int initval);
    void releaseSEM(int id);

    int initMSQ(int key);
    void releaseMSQ(int id);

    const int MBUF_SIZE = 1024;
    struct IPCMessage
    {
        long mtype;
        char mtext[MBUF_SIZE];
    };
        // Communications structure between local
        // and external procs via SysV message queues

    const long MTYPE = 1L;
    int sendToMessageQ(const char *msg, long mtype, int msqid);
        // Used to send a message to the given SysV message queue
};
#endif
