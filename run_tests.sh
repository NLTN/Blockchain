#!/bin/bash

rm *.class
# rm BlockChain.class
javac -cp blockChainGrader.jar:algs4.jar:rsa.jar:. DropboxTestBlockChain.java
java -cp blockChainGrader.jar:rsa.jar:. DropboxTestBlockChain