'''
Created on May 8, 2018

@author: acaproni
'''

import socket

UDP_IP = "127.0.0.1"
UDP_PORT = 5000
MESSAGE = "Ciao, Ale!"

if __name__ == '__main__':
    print ("UDP target IP:", UDP_IP)
    print ("UDP target port:", UDP_PORT)
    print ("message:", MESSAGE)
    
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.sendto(bytes(MESSAGE, "utf-8"),(UDP_IP, UDP_PORT))
