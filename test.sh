#!/bin/sh
export CLASSPATH=/mnt/d
for i in $(ls ./Scanner/*.vc);
    do
    java VC.vc $i > ${i%.*}.s;
    diff ${i%.*}.sol ${i%.*}.s;
    rm ${i%.*}.s;
    done;
