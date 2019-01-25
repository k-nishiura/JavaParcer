import glob
import sys, os
from io import StringIO
from pathlib import Path
import logging.config
from  ast.ast_processor import AstProcessor
from ast.basic_info_listener import BasicInfoListener

class pycolor:
    BLACK = '\033[30m'
    RED = '\033[31m'
    GREEN = '\033[32m'
    YELLOW = '\033[33m'
    BLUE = '\033[34m'
    PURPLE = '\033[35m'
    CYAN = '\033[36m'
    WHITE = '\033[37m'
    END = '\033[0m'
    BOLD = '\038[1m' 
    UNDERLINE = '\033[4m'
    INVISIBLE = '\033[08m'
    REVERCE = '\033[07m'

def red(s):
    return pycolor.RED + s + pycolor.END
def blue(s):
    return pycolor.BLUE + s + pycolor.END
def yellow(s):
    return pycolor.YELLOW + s + pycolor.END

def redirect_stdout():
    sys.stdout.flush()






path = "./target/src/test/java/jp/kusumotolab/kgenprog/"

logging_setting_path = './resources/logging/utiltools_log.conf'


def parse(path, depth):
    sys.stdout = StringIO()
    logging.config.fileConfig(logging_setting_path)
    logger = logging.getLogger(__file__)
    target_file_path =  path
    ast_info = AstProcessor(logging, BasicInfoListener()).execute(target_file_path)
    sys.stdout = sys.__stdout__
    #print(ast_info)
    print("   "*depth, blue(ast_info["className"]))
    for method in ast_info["methods"]:
        print("   "*depth, yellow(method["methodName"]))

    text = open(path,"r")
    text = [i.strip() for i in text]
    #print(text)
    assertText = [i for i in text if i[:6]=="assert"]
    for i in assertText: print(i)
    

def explore(path, depth):
    files = os.listdir(path)

    java_files_dir = [f for f in files if f[-4:]=="java"]
    #print(java_files_dir)
    for jfile in java_files_dir:
        print("   "*depth, red(jfile))
        parse(path+jfile, depth+1)
        

    files_dir = [f for f in files if os.path.isdir(os.path.join(path, f))]
    #print(files_dir)    
    for dirname in files_dir:
        print("   "*depth, dirname+"/")
        dpath = path+dirname+"/"
        explore(dpath, depth+1)

explore(path,0)
