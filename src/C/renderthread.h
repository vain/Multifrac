#include <stdbool.h>

/* To carry informations from the main thread to a node. */
struct nodeinfo
{
	int sock;
	int ID;
	int numthreads;
};

/* Port of ColorStep. */
struct ColorStep
{
	float pos;
	int R;
	int G;
	int B;
};

/* Port of FractalParameters + Job. These two can collapse into one
 * structure as there's no sharing of a FractalParameters object in a
 * render node. */
struct FractalParameters
{
	int type;
	double escape;
	int nmax;
	bool adaptive;
	double zoom;
	double centerX, centerY;
	double juliaRE, juliaIM;

	/* colorInside as BGRA. */
	int colorInsideBGRA;

	/* Number of colors and the gradient array. Cache the very last
	 * color as a BGRA int. */
	int gradLen;
	struct ColorStep *grad;
	int colorLastBGRA;

	/* Size and buffer. */
	int w, h;
	int rows;
	int *buf;

	/* Job infos. */
	int start, end;
};

/* Launch a new node for this socket. */
int launchNode(int sock, int numthreads);
