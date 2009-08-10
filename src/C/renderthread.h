#include <stdbool.h>

/* To carry informations from the main thread to a node. */
struct nodeinfo
{
	int sock;
	int ID;
};

/* Port of ColorStep. */
struct ColorStep
{
	float pos;
	int R;
	int G;
	int B;
};

/* Port of FractalParameters + size and buffer. */
struct FractalParameters
{
	int type;
	double escape;
	int nmax;
	bool adaptive;
	double zoom;
	double centerX, centerY;
	double juliaRE, juliaIM;

	/* colorInside as RGB. */
	int colorInsideR;
	int colorInsideG;
	int colorInsideB;

	/* Number of colors in the gradient array. */
	int gradLen;
	struct ColorStep *grad;

	/* Size and buffer. */
	int w, h;
	int *buf;
};

/* Launch a new node for this socket. */
int launchNode(int sock);
