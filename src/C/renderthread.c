/* Standard */
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

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

#define TYPE_MANDELBROT 0
#define TYPE_JULIA      1

#define M() printf("[%d] ", ni->ID)
#define E() fprintf(stderr, "[%d] ", ni->ID)

#define CHECKDIE									\
		if (res == -1)								\
		{											\
			killSock(ni->sock);						\
			M(); printf("Thread quitting.\n");		\
			free(ni);								\
			free(param.grad);						\
			free(param.buf);						\
			pthread_exit(NULL);						\
		}

#define CHECKRET			\
		if (res == -1)		\
			return -1;


/* Render (a part) of a fractal. */
void renderFractal(struct FractalParameters *param)
{
	double logTwoBaseTen = log10(2.0);

	// Mandelbrot Parameters
	double x, y;
	double Re_c, Im_c;
	double Re_z, Im_z, Re_z2, Im_z2;
	double sqr_abs_z;
	double escape = param->escape;
	int n = 0;
	int nmax = param->nmax;
	double w = param->w;
	double muh = 0.0;
	int index = 0;

	int coord_y, coord_x, i;

	for (coord_y = param->start; coord_y < param->end; coord_y++)
	{
		//y = myJob.param.YtoWorld(coord_y);
		y = 2.0 * (double)coord_y / (double)param->h;
		y -= 1.0;
		y *= param->zoom;
		y += param->centerY;
		// ---

		for (coord_x = 0; coord_x < w; coord_x++)
		{
			//x = myJob.param.XtoWorld(coord_x);
			x = 2.0 * (double)coord_x / (double)param->h;
			x -= ((double)param->w / (double)param->h);
			x *= param->zoom;
			x += param->centerX;
			// ---

			// Prerequisites
			Re_c = x;
			Im_c = y;
			Re_z = Im_z = Re_z2 = Im_z2 = sqr_abs_z = 0.0;

			switch (param->type)
			{
				case TYPE_MANDELBROT:
					// z_{n+1} = z_n^2 + c ,  z_0 = 0 ,
					// c the coordinate
					Re_c = x;
					Im_c = y;
					break;
				case TYPE_JULIA:
					// z_{n+1} = z_n^2 + k ,  z_0 = c ,
					// c coord., k Julia-Param.
					Re_c = param->juliaRE;
					Im_c = param->juliaIM;
					Re_z = x;
					Im_z = y;
					break;
			}

			n = 0;

			// Loop
			Re_z2 = Re_z * Re_z;
			Im_z2 = Im_z * Im_z;
			while (sqr_abs_z < escape && n < nmax)
			{
				Im_z = 2.0 * Re_z * Im_z + Im_c;
				Re_z = Re_z2 - Im_z2 + Re_c;

				Re_z2 = Re_z * Re_z;
				Im_z2 = Im_z * Im_z;

				sqr_abs_z = Re_z2 + Im_z2;
				n++;
			}

			// Decision
			if (n == nmax)
			{
				// Inside
				param->buf[index++] = param->colorInsideBGRA;
			}
			else
			{
				// Outside
				// Idea: http://linas.org/art-gallery/escape/smooth.html
				muh = (double)n + 1.0f -
					log10(log10(sqrt(sqr_abs_z))) / logTwoBaseTen;
				muh /= nmax;

				// Linear interpolation between marks
				if (muh >= 1.0)
				{
					// If muh is greater than or equal to 1, just use the
					// last color.
					param->buf[index++] = param->colorLastBGRA;
				}
				else
				{
					i = 1;

					// Find the first index where muh will be less than
					// get(i). This will be (i + 1), so decrease i
					// afterwards.
					while (i < param->gradLen && muh > param->grad[i].pos)
						i++;

					i--;

					// Scale muh from 0 to 1 in the given interval.
					double span = param->grad[i + 1].pos
						- param->grad[i].pos;

					muh -= param->grad[i].pos;
					muh /= span;

					// Get the 2 colors and interpolate them linearly.
					int R1 = param->grad[i].R;
					int G1 = param->grad[i].G;
					int B1 = param->grad[i].B;
					int R2 = param->grad[i + 1].R;
					int G2 = param->grad[i + 1].G;
					int B2 = param->grad[i + 1].B;

					int R = (int)(R1 * (1.0 - muh)) + (int)(R2 * muh);
					int G = (int)(G1 * (1.0 - muh)) + (int)(G2 * muh);
					int B = (int)(B1 * (1.0 - muh)) + (int)(B2 * muh);

					// Convert it back to a BGRA-integer.
					param->buf[index++] =
						  (B << 24)
						+ (G << 16)
						+ (R << 8)
						+ 0xFF;
				}
			}
		}
	}
}

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
	res = readIntKeep(s, &(p->colorInsideBGRA));	CHECKRET;

	/* Color gradient */
	res = readInt(s, &(p->gradLen));		CHECKRET;
	if (p->gradLen > 0)
	{
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

		/* Cache last color as BGRA. */
		p->colorLastBGRA =
			  (p->grad[p->gradLen - 1].B << 24)
			+ (p->grad[p->gradLen - 1].G << 16)
			+ (p->grad[p->gradLen - 1].R << 8)
			+ 0xFF;
	}
	else
	{
		p->grad = NULL;
		p->colorLastBGRA = 0;
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

	printf("\tcolI = %X\n", p->colorInsideBGRA);
	printf("\tcolL = %X\n", p->colorLastBGRA);

	if (p->grad != NULL && p->gradLen > 0)
	{
		printf("\n");
		for (i = 0; i < p->gradLen; i++)
		{
			printf("\tpos = %lf\n", p->grad[i].pos);
			printf("\tR   = %X\n", p->grad[i].R);
			printf("\tG   = %X\n", p->grad[i].G);
			printf("\tB   = %X\n", p->grad[i].B);
		}
		printf("\n");
	}

	printf("\tw = %d, h = %d\n", p->w, p->h);
}

/* A thread to handle all jobs on its socket. */
static void *renderthread(void *info)
{
	int res, cmd, buf;
	struct nodeinfo *ni = (struct nodeinfo *)info;
	struct FractalParameters param;

	/* Detach yourself so no one waits for your return. */
	pthread_detach(pthread_self());

	/* Init */
	param.gradLen = 0;
	param.grad = NULL;
	param.buf  = NULL;

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
				res = -1;
				CHECKDIE;
				break;

			case CMD_PING:
				res = readInt(ni->sock, &buf);
				CHECKDIE;
				M(); printf("PONG: %d\n", buf);

				res = writeInt(ni->sock, buf + 1);
				CHECKDIE;
				break;

			case CMD_ADCPUS:
				M(); printf("Advertising number of processors.\n");
				res = writeInt(ni->sock, ni->numthreads);
				CHECKDIE;
				break;

			case CMD_PARAM:
				M(); printf("Receiving FractalParameters and size...\n");
				res = readParams(&param, ni->sock);
				CHECKDIE;

				dumpParams(&param);
				break;

			case CMD_ROWS:
				M(); printf("Receiving row count... ");
				res = readInt(ni->sock, &(param.rows));
				CHECKDIE;

				printf("%d rows.\n", param.rows);

				if (param.buf != NULL)
				{
					free(param.buf);
					param.buf = NULL;
				}

				if (param.rows * param.w > 0)
				{
					param.buf = (int *)malloc(
							sizeof(int) * param.w * param.rows);
					M(); printf("Done. Buffer size: %d\n",
							sizeof(int) * param.w * param.rows);
				}
				else
				{
					E(); fprintf(stderr,
							"Oops. Buffer would be 0 bytes long.\n");
				}

				break;

			case CMD_JOB:
				if (param.buf == NULL)
				{
					E(); fprintf(stderr, "Fatal: Job received but "
							"you haven't allocated a buffer.\n");
					res = -1;
					CHECKDIE;
				}

				if (param.grad == NULL)
				{
					E(); fprintf(stderr, "Fatal: Job received but "
							"you haven't allocated a gradient.\n");
					res = -1;
					CHECKDIE;
				}

				M(); printf("Receiving TokenSettings...\n");
				res = readInt(ni->sock, &(param.start));
				CHECKDIE;

				res = readInt(ni->sock, &(param.end));
				CHECKDIE;

				M(); printf("Okay. Rendering: %d, %d\n",
						param.start, param.end);

				renderFractal(&param);

				/* Note: The renderer already took care of the right
				 * endianess, so we don't need a conversion. */
				M(); printf("Rendering done, sending image...\n");
				res = writeIntBulkKeep(ni->sock, param.buf,
						(param.end - param.start) * param.w);
				CHECKDIE;

				M(); printf("Finished this token.\n");

				break;

			default:
				E(); fprintf(stderr, "Unknown command.\n");
		}
	}
}

/* Launch a new node for this socket. */
int launchNode(int sock, int numthreads)
{
	int res;
	pthread_t child;
	struct nodeinfo *ni;
	static int newID = 0;
	
	ni = (struct nodeinfo *)malloc(sizeof(struct nodeinfo));
	ni->sock = sock;
	ni->ID = newID++;
	ni->numthreads = numthreads;

	res = pthread_create(&child, NULL, renderthread, (void *)ni);
	if (res != 0)
	{
		errno = res;
		perror("Could not start thread");
		return -1;
	}

	return 0;
}
