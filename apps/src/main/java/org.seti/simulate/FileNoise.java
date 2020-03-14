package org.seti.simulate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileNoise extends ArrayNoise {
    public FileNoise(String dataFileName) throws IOException {
        super(dataFileName, Files.readAllBytes((Paths.get(dataFileName))));
    }
}
