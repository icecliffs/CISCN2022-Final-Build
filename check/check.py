# -*- coding:utf8 -*-
import sys
import re
from redis import Redis
import socket
from requests import *
import time

#除了返回的正常/错误，其余不输出任何信息，且需要设置超时时间！使用python3进行编写

# 检查语句
def check1(url):
    try:
        r = get(url=url)
        if(r.status_code == 200):
            print("[*] Check1 Successful!")
            return True
        else:
            return False
    except Exception as e:
        print("[!] Check1 Error!")
    return True

# 检查flag接口是否存在
def check2(url):
    try:
        r = get(url=url+"/gifts")
        if(r.status_code == 200):
            print("[*] Check2 Successful!")
            return True
        else:
            return False
    except Exception as e:
        print("[!] Check2 Error!")
    return True

# 控制语句
def checker(host, port):
    print("")
    try:
        url = "http://"+host+":"+str(port)
        if check1(url) and check2(url):
            return True
            # return (True,"status": "up", "msg": "OK")  # check成功返回字段。"status": "up", "msg": "OK"
    except Exception as e:
        return False
        # return (False, "status": "down", "msg": "ERROR")  # check不成功返回字段。"status": "down", "msg": "ERROR"

# 主逻辑
if __name__ == '__main__':
    ip=sys.argv[1]
    port=int(sys.argv[2])
    for i in range(0, 50):
        i += 1
        print("[!] 运行第{0}次，状态为：".format(i))
        print(checker(ip,port))
        time.sleep(1)