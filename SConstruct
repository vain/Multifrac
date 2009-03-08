# -*- coding: utf8 -*-

import os

# Sonst findet er den javac nicht, vermutlich weil der nicht unter
# /usr/bin liegt - LANG ist wegen UTF8
env = Environment(ENV = {
	'PATH' : os.environ['PATH'],
	'LANG' : os.environ['LANG']})

env.Java('build', '.')
