/* Sockets */
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>

/* close() */
#include <unistd.h>

/* Endianess */
#include <endian.h>

/* memcpy() */
#include <string.h>

/* Project related */
#include "sockfuncs.h"


/* Read 4 bytes and convert their endianess. Java writes big endian, so
 * convert it to whatever this host uses. */
int readInt(int sock, int *buf)
{
	ssize_t res;

	res = recv(sock, buf, 4, MSG_WAITALL);
	if (res <= 0)
		return -1;

	*buf = be32toh(*buf);
	return 0;
}

/* Write 4 bytes with the right endianess. */
int writeInt(int sock, int val)
{
	int buf = htobe32(val);

	if (send(sock, &buf, 4, MSG_NOSIGNAL) == -1)
		return -1;
	else
		return 0;
}

/* Read 8 bytes as a double. */
int readDouble(int sock, double *buf)
{
	ssize_t res;
	long long unsigned int local;

	res = recv(sock, &local, 8, MSG_WAITALL);
	if (res <= 0)
		return -1;

	/* We cannot just "swap" the double-value. So swap a long value with
	 * 64 bits and then copy those bits to the output buffer. */
	local = be64toh(local);
	memcpy(buf, &local, 8);
	return 0;
}

/* Write a double as 8 bytes. */
int writeDouble(int sock, double d)
{
	/* Same as above... */
	long long int local = 0;
	memcpy(&local, &d, 8);
	local = htobe64(local);

	if (send(sock, &local, 8, MSG_NOSIGNAL) == -1)
		return -1;
	else
		return 0;
}

/* Read 8 bytes as a float. */
int readFloat(int sock, float *buf)
{
	ssize_t res;
	int local;

	res = recv(sock, &local, 4, MSG_WAITALL);
	if (res <= 0)
		return -1;

	/* We cannot just "swap" the float-value. So swap a long value with
	 * 32 bits and then copy those bits to the output buffer. */
	local = be32toh(local);
	memcpy(buf, &local, 4);
	return 0;
}

/* Write a float as 8 bytes. */
int writeFloat(int sock, float d)
{
	/* Same as above... */
	int local = 0;
	memcpy(&local, &d, 4);
	local = htobe32(local);

	if (send(sock, &local, 4, MSG_NOSIGNAL) == -1)
		return -1;
	else
		return 0;
}

/* Read 1 byte as a boolean. */
int readBoolean(int sock, bool *buf)
{
	ssize_t res;

	res = recv(sock, buf, 1, MSG_WAITALL);
	if (res <= 0)
		return -1;
	else
		return 0;
}

/* Write 1 byte as a boolean. */
int writeBoolean(int sock, bool val)
{
	if (send(sock, &val, 1, MSG_NOSIGNAL) == -1)
		return -1;
	else
		return 0;
}

/* Try to shutdown and then close this socket. */
void killSock(int sock)
{
	shutdown(sock, SHUT_RDWR);
	close(sock);
}
