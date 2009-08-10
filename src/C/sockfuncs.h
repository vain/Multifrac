#include <stdbool.h>

int readInt(int sock, int *buf);
int writeInt(int sock, int val);

int readDouble(int sock, double *buf);
int writeDouble(int sock, double val);

int readFloat(int sock, float *buf);
int writeFloat(int sock, float val);

int readBoolean(int sock, bool *buf);
int writeBoolean(int sock, bool val);

void killSock(int sock);
