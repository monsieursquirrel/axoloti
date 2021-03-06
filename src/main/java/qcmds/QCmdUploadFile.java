/**
 * Copyright (C) 2013, 2014 Johannes Taelman
 *
 * This file is part of Axoloti.
 *
 * Axoloti is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Axoloti is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Axoloti. If not, see <http://www.gnu.org/licenses/>.
 */
package qcmds;

import axoloti.Connection;
import axoloti.SDCardInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdUploadFile implements QCmdSerialTask {

    InputStream inputStream;
    final String filename;
    File file;
    long size;
    long tsEpoch;

    public QCmdUploadFile(InputStream inputStream, String filename) {
        this.inputStream = inputStream;
        this.filename = filename;
    }

    public QCmdUploadFile(File file, String filename) {
        this.file = file;
        this.filename = filename;
        inputStream = null;
    }

    @Override
    public String GetStartMessage() {
        return "Start uploading file to sdcard : " + filename;
    }

    @Override
    public String GetDoneMessage() {
        return "Done uploading file";
    }

    @Override
    public QCmd Do(Connection connection) {
        connection.ClearSync();
        try {
            if (inputStream == null) {
                inputStream = new FileInputStream(file);
            }
            Logger.getLogger(QCmdUploadFile.class.getName()).log(Level.INFO, "uploading: {0}", filename);
            Calendar ts = Calendar.getInstance();
            if (file != null) {
                ts.setTimeInMillis(file.lastModified());
            }
            int tlength = inputStream.available();
            int remLength = inputStream.available();
            size = tlength;
            connection.TransmitCreateFile(filename, tlength, ts);
            int MaxBlockSize = 32768;
            int pct = 0;
            do {
                int l;
                if (remLength > MaxBlockSize) {
                    l = MaxBlockSize;
                    remLength -= MaxBlockSize;
                } else {
                    l = remLength;
                    remLength = 0;
                }
                byte[] buffer = new byte[l];
                int nRead = inputStream.read(buffer, 0, l);
                if (nRead != l) {
                    Logger.getLogger(QCmdUploadFile.class.getName()).log(Level.SEVERE, "file size wrong?{0}", nRead);
                }
                connection.TransmitAppendFile(buffer);
                int newpct = (100 * (tlength - remLength) / tlength);
                if (newpct != pct) {
                    Logger.getLogger(QCmdUploadFile.class.getName()).log(Level.INFO, "uploading : {0}%", newpct);
                }
                pct = newpct;
                remLength = inputStream.available();
            } while (remLength > 0);

            inputStream.close();
            connection.TransmitCloseFile();
            
            SDCardInfo.getInstance().AddFile(filename, (int) size, 0);
            
            return this;
        } catch (IOException ex) {
            Logger.getLogger(QCmdUploadFile.class.getName()).log(Level.SEVERE, "IOException", ex);
        }
        return new QCmdDisconnect();
    }

}
