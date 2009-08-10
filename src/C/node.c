/* Standard */
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>

/* Sockets */
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>

/* close() */
#include <unistd.h>

/* memset() */
#include <string.h>

/* Project related */
#include "renderthread.h"


static int server;

static void cleanup(int sig)
{
	shutdown(server, SHUT_RDWR);
	close(server);
	exit(EXIT_SUCCESS);
}

/* Print a sockaddr struct as "host", "service". */
static void print_sockaddr(struct sockaddr *sa, size_t salen)
{
	char host[NI_MAXHOST] = "";
	char serv[NI_MAXSERV] = "";
	int res = 0;

	res = getnameinfo(sa, salen,
			host, NI_MAXHOST,
			serv, NI_MAXSERV,
			NI_NUMERICHOST | NI_NUMERICSERV);

	if (res != 0)
	{
		fprintf(stderr, "Error in getnameinfo: %s\n", gai_strerror(res));
		return;
	}

	printf("- Address: %s, %s\n", host, serv);
}

/* Create a server socket, bind and listen. */
static int createSocket(char *host, char *port)
{
	int res, sock;
	struct addrinfo hints;
	struct addrinfo *ai_res, *ai_ptr;

	printf("Creating server socket.\n");

	/* We want a TCP socket. */
	memset(&hints, 0, sizeof(struct addrinfo));
	hints.ai_family = AF_UNSPEC;
	hints.ai_socktype = SOCK_STREAM;
	if ((res = getaddrinfo(host, port, &hints, &ai_res)) != 0)
	{
		fprintf(stderr, "Error in getaddrinfo: %s\n", gai_strerror(res));
		return -1;
	}

	/* Find a matching interface and bind to it. */
	printf("Binding...\n");
	for (ai_ptr = ai_res; ai_ptr != NULL; ai_ptr = ai_ptr->ai_next)
	{
		print_sockaddr(ai_ptr->ai_addr, ai_ptr->ai_addrlen);

		sock = socket(ai_ptr->ai_family,
				ai_ptr->ai_socktype,
				ai_ptr->ai_protocol);

		if (sock == -1)
		{
			perror("Failed");
			continue;
		}

		if (bind(sock, ai_ptr->ai_addr, ai_ptr->ai_addrlen) != -1)
			break;

		close(sock);
	}

	freeaddrinfo(ai_res);

	if (ai_ptr == NULL)
	{
		fprintf(stderr, "Could not find an interface to bind to.\n");
		return -1;
	}

	/* Listen... */
	printf("Listening...\n");
	if (listen(sock, 15) == -1)
	{
		perror("Could not listen");
		close(sock);
		return -1;
	}

	printf("Success.\n");
	return sock;
}

int main(int argc, char **argv)
{
	int client;
	struct sockaddr client_addr;
	size_t client_addr_len = sizeof(client_addr);

	/* Arguments */
	char *host = "localhost";
	char *port = "7331";
	int threads = sysconf(_SC_NPROCESSORS_ONLN);
	int i;

	/* sysconf succeeded? */
	if (threads < 1)
		threads = 1;

	for (i = 1; i < argc; i++)
	{
		if (strncmp(argv[i], "-h", 2) == 0)
			host = argv[++i];
		else if (strncmp(argv[i], "-p", 2) == 0)
			port = argv[++i];
		else if (strncmp(argv[i], "-t", 2) == 0)
			threads = atoi(argv[++i]);
		else
		{
			fprintf(stderr,
					"Usage: %s [-h host] [-p port] [-t threads] [--help]\n",
					argv[0]);
			exit(EXIT_FAILURE);
		}
	}

	if (threads < 1)
	{
		fprintf(stderr,
				"You requested less than 1 thread. That's not useful.\n");
		exit(EXIT_FAILURE);
	}

	printf("Configured options:\n"
			"\tthreads = %d\n", threads);

	/* Add signal handler */
	if (signal(SIGINT, cleanup) == SIG_ERR)
	{
		perror("Could not add signal handler for SIGINT");
	}

	/* Create server socket */
	server = createSocket(host, port);
	if (server == -1)
		exit(EXIT_FAILURE);

	/* Main server loop. */
	while (1)
	{
		if ((client = accept(server, &client_addr, &client_addr_len)) == -1)
		{
			perror("Could not accept");
			close(client);
			close(server);
			exit(EXIT_FAILURE);
		}

		printf("New client, launching node:\n");
		print_sockaddr(&client_addr, client_addr_len);
		launchNode(client, threads);
	}

	exit(EXIT_SUCCESS);
}
