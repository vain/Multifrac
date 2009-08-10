# -*- coding: utf-8 -*-

env = Environment(CCFLAGS = '-Wall -O2 -march=i686')
env.Program('node', Glob('src/C/*.c'), LIBS = ['pthread', 'm'])
