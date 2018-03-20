#!/bin/sh
for i in $(ls ./Recogniser/*.vc);
    do
    echo $i;
    java VC.vc $i > ${i%.*}.s;
    diff ${i%.*}.sol ${i%.*}.s;
    rm ${i%.*}.s;
    done;
