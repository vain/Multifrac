/* Standard */
#include <stdio.h>
#include <stdlib.h>

/* Threads */
#include <pthread.h>
#include <errno.h>

/* Project related */
#include "renderthread.h"
#include "sockfuncs.h"


#define CMD_CLOSE  0
#define CMD_PING   1
#define CMD_ADCPUS 2

#define CMD_PARAM 1000
#define CMD_ROWS  1010
#define CMD_JOB   1100

#define M() printf("[%d] ", ni->ID)
#define E() fprintf(stderr, "[%d] ", ni->ID)

#define CHECKDIE									\
		if (res == -1)								\
		{											\
			killSock(ni->sock);						\
			M(); printf("Thread quitting.\n");		\
			free(ni);								\
			free(param.grad);						\
			return NULL;							\
		}

#define CHECKRET			\
		if (res == -1)		\
			return -1;


/* Read fractal parameters from a socket. */
static int readParams(struct FractalParameters *p, int s)
{
	int i;
	int dummy = 0;
	int res;

	/* Version */
	res = readInt(s, &dummy);				CHECKRET;

	/* Basic properties */
	res = readInt(s, &(p->type));			CHECKRET;
	res = readDouble(s, &(p->escape));		CHECKRET;
	res = readInt(s, &(p->nmax));			CHECKRET;
	res = readBoolean(s, &(p->adaptive));	CHECKRET;
	res = readDouble(s, &(p->zoom));		CHECKRET;

	/* Center offset, julia and "inside" color */
	res = readDouble(s, &(p->centerX));		CHECKRET;
	res = readDouble(s, &(p->centerY));		CHECKRET;
	res = readDouble(s, &(p->juliaRE));		CHECKRET;
	res = readDouble(s, &(p->juliaIM));		CHECKRET;
	res = readInt(s, &dummy);				CHECKRET;
	p->colorInsideR = ((dummy & 0x00FF0000) >> 16);
	p->colorInsideG = ((dummy & 0x0000FF00) >> 8);
	p->colorInsideB =  (dummy & 0x000000FF);

	/* Color gradient */
	res = readInt(s, &(p->gradLen));		CHECKRET;
	p->grad = (struct ColorStep *)malloc(
			sizeof(struct ColorStep) * p->gradLen);

	for (i = 0; i < p->gradLen; i++)
	{
		/* ColorStep version -- ignore */
		res = readInt(s, &dummy);			CHECKRET;

		/* Position, RGB color */
		res = readFloat(s, &(p->grad[i].pos));	CHECKRET;
		res = readInt(s, &dummy);				CHECKRET;
		p->grad[i].R = ((dummy & 0x00FF0000) >> 16);
		p->grad[i].G = ((dummy & 0x0000FF00) >> 8);
		p->grad[i].B =  (dummy & 0x000000FF);
	}

	/* Read image size */
	res = readInt(s, &(p->w));			CHECKRET;
	res = readInt(s, &(p->h));			CHECKRET;

	return 0;
}

static void dumpParams(struct FractalParameters *p)
{
	int i;

	printf("\ttype = %d\n", p->type);
	printf("\tesca = %lf\n", p->escape);
	printf("\tnmax = %d\n", p->nmax);
	printf("\tadap = %d\n", p->adaptive);
	printf("\tzoom = %.16lf\n", p->zoom);

	printf("\tcenX = %.16lf\n", p->centerX);
	printf("\tcenY = %.16lf\n", p->centerY);
	printf("\tjulR = %.16lf\n", p->juliaRE);
	printf("\tjulI = %.16lf\n", p->juliaIM);

	printf("\tcoIR = %X\n", p->colorInsideR);
	printf("\tcoIG = %X\n", p->colorInsideG);
	printf("\tcoIB = %X\n", p->colorInsideB);

	printf("\n");
	for (i = 0; i < p->gradLen; i++)
	{
		printf("\tpos = %lf\n", p->grad[i].pos);
		printf("\tR   = %X\n", p->grad[i].R);
		printf("\tG   = %X\n", p->grad[i].G);
		printf("\tB   = %X\n", p->grad[i].B);
	}
	printf("\n");

	printf("\tw = %d, h = %d\n", p->w, p->h);
}

/* A thread to handle all jobs on its socket. */
static void *renderthread(void *info)
{
	int res, cmd, buf;
	struct nodeinfo *ni = (struct nodeinfo *)info;
	struct FractalParameters param;

	/* Init */
	param.gradLen = 0;
	param.grad = NULL;

	M(); printf("Thread running.\n");

	while (1)
	{
		res = readInt(ni->sock, &cmd);
		CHECKDIE;

		M(); printf("cmd = %d\n", cmd);
		switch (cmd)
		{
			case CMD_CLOSE:
				M(); printf("Closing as requested.\n");
				killSock(ni->sock);
				return NULL;

			case CMD_PING:
				res = readInt(ni->sock, &buf);
				CHECKDIE;
				M(); printf("PONG: %d\n", buf);

				res = writeInt(ni->sock, buf + 1);
				CHECKDIE;
				break;

			case CMD_ADCPUS:
				M(); printf("Advertising number of processors.\n");
				res = writeInt(ni->sock, 1);
				CHECKDIE;
				break;

			case CMD_PARAM:
				M(); printf("Receiving FractalParameters and size...\n");
				res = readParams(&param, ni->sock);
				CHECKDIE;
				dumpParams(&param);
				break;

			/*
			case 8:
			{
				double d;

				M(); printf("DOUBLE TEST\n");
				res = readDouble(ni->sock, &d);
				CHECKDIE;

				printf("%.64lf\n", d);

				res = writeDouble(ni->sock, d);
				CHECKDIE;
				break;
			}

			case 9:
			{
				bool b;

				M(); printf("BOOLEAN TEST, %d\n", sizeof(bool));
				res = readBoolean(ni->sock, &b);
				CHECKDIE;

				printf("%d\n", b);

				res = writeBoolean(ni->sock, b);
				CHECKDIE;
				break;
			}

			case 10:
			{
				float f;

				M(); printf("FLOAT TEST\n");
				res = readFloat(ni->sock, &f);
				CHECKDIE;

				printf("%.64f\n", f);

				res = writeFloat(ni->sock, f);
				CHECKDIE;
				break;
			}
			*/

			default:
				E(); fprintf(stderr, "Unknown command.\n");
		}
	}

	return NULL;
}

/* Launch a new node for this socket. */
int launchNode(int sock)
{
	int res;
	pthread_t child;
	struct nodeinfo *ni;
	static int newID = 0;
	
	ni = (struct nodeinfo *)malloc(sizeof(struct nodeinfo));
	ni->sock = sock;
	ni->ID = newID++;

	res = pthread_create(&child, NULL, renderthread, (void *)ni);
	if (res != 0)
	{
		errno = res;
		perror("Could not start thread");
		return -1;
	}

	return 0;
}
