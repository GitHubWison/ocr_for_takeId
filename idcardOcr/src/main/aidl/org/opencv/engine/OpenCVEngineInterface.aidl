package org.opencv.engine;


interface OpenCVEngineInterface
{

    int getEngineVersion();


    String getLibPathByVersion(String version);


    boolean installVersion(String version);


    String getLibraryList(String version);
    String test(String abc);
}
