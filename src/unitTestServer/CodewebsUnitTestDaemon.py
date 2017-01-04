#!/usr/bin/env python

import os.path
import sys
import logging

configFile = os.path.join(os.path.dirname(os.path.realpath(__file__)),'localconfig')
config = {}
with open(configFile) as fid:
    rows = fid.readlines()
    for r in rows:
        row = r.strip().split()
        config[row[0]] = row[1]
HWID = int(config['HW'])
PARTID = int(config['PART'])
LOCALPATH = config['PATH']
INSTALLPATH = config['INSTALLPATH']
HOST = config['HOST']
sys.path.append(LOCALPATH)
from src.octaveUnitTesting.UnitTester import UnitTester
import pika


class CodewebsUnitTestDaemon(object):
    def __init__(self):
        logdir = os.path.join(INSTALLPATH,'log', 'UnitTestServer')
        logfilename = os.path.join(logdir,'log_' + str(HWID) + '_' + str(PARTID) + '.log')
        logging.basicConfig(filename=logfilename, format='%(asctime)s %(message)s', \
                            datefmt='%m/%d/%Y %I:%M:%S %p', level=logging.DEBUG)
    

        logging.debug('Setting up connection.')
        self.connection = pika.BlockingConnection(pika.ConnectionParameters(
            host=HOST))
        self.channel = self.connection.channel()
        self.channel.queue_declare(queue='codewebs_unittest_queue')
    
        self.tester = UnitTester(HWID, PARTID)
        self.tester.refreshWorkingDir()
        logging.debug('Ready to rumble!')

    def onRequest(self, ch, method, props, body):
        logging.debug(' [.] Request received, running unit tests.')
        self.tester.loadCode(body)
        output, correct = self.tester.run()
    
        ch.basic_publish(exchange='',
                        routing_key=props.reply_to,
                        properties=pika.BasicProperties(correlation_id = \
                                                     props.correlation_id),
                        body=str(correct))
        if correct == True:
            logging.debug('\t\t... Result: passed!')
        else:
            logging.debug('\t\t... Result: failed!')
        ch.basic_ack(delivery_tag = method.delivery_tag)
        self.tester.refreshWorkingDir()

    def run(self):
        self.channel.basic_qos(prefetch_count=1)
        self.channel.basic_consume(self.onRequest, queue='codewebs_unittest_queue')

        logging.debug(' [x] Awaiting RPC requests')
        self.channel.start_consuming()

if __name__ == '__main__':
    d = CodewebsUnitTestDaemon()
    d.run()



