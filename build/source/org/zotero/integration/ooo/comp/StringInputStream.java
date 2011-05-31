/*
    ***** BEGIN LICENSE BLOCK *****
	
	Copyright (c) 2009  Zotero
	                    Center for History and New Media
						George Mason University, Fairfax, Virginia, USA
						http://zotero.org
	
	Zotero is free software: you can redistribute it and/or modify
	it under the terms of the GNU Affero General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	Zotero is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Affero General Public License for more details.
	
	You should have received a copy of the GNU Affero General Public License
	along with Zotero.  If not, see <http://www.gnu.org/licenses/>.
    
    ***** END LICENSE BLOCK *****
*/

package org.zotero.integration.ooo.comp;

import java.io.ByteArrayInputStream;

import com.sun.star.io.BufferSizeExceededException;
import com.sun.star.io.NotConnectedException;
import com.sun.star.io.XInputStream;
import com.sun.star.io.XSeekable;

/**
* <a href="http://www.oooforum.org/forum/viewtopic.phtml?t=13205">OOInputStream from the thread <b>OOo-Java: Using XInputStream...</b></a>
*/
public class StringInputStream extends ByteArrayInputStream implements XInputStream, XSeekable {

    public StringInputStream(byte[] buf) {
        super(buf);
    }


    //
    // Implement XInputStream
    //

    public int readBytes(byte[][] buffer, int bufferSize) throws NotConnectedException, BufferSizeExceededException, com.sun.star.io.IOException {
        int numberOfReadBytes;
        try {
            byte[] bytes = new byte[bufferSize];
            numberOfReadBytes = super.read(bytes);
            if(numberOfReadBytes > 0) {
                if(numberOfReadBytes < bufferSize) {
                    byte[] smallerBuffer = new byte[numberOfReadBytes];
                    System.arraycopy(bytes, 0, smallerBuffer, 0, numberOfReadBytes);
                    bytes = smallerBuffer;
                }
            }
            else {
                bytes = new byte[0];
                numberOfReadBytes = 0;
            }

            buffer[0]=bytes;
            return numberOfReadBytes;
        }
        catch (java.io.IOException e) {
            throw new com.sun.star.io.IOException(e.getMessage(),this);
        }
    }

    public int readSomeBytes(byte[][] buffer, int bufferSize) throws NotConnectedException, BufferSizeExceededException, com.sun.star.io.IOException {
        return readBytes(buffer, bufferSize);
    }

    public void skipBytes(int skipLength) throws NotConnectedException, BufferSizeExceededException, com.sun.star.io.IOException {
        skip(skipLength);
    }

    public void closeInput() throws NotConnectedException, com.sun.star.io.IOException {
        try {
            close();
        }
        catch (java.io.IOException e) {
            throw new com.sun.star.io.IOException(e.getMessage(), this);
        }
    }


    //
    // Implement XSeekable
    //

    public long getLength() throws com.sun.star.io.IOException {
        return count;
    }

    public long getPosition() throws com.sun.star.io.IOException {
        return pos;
    }

    public void seek(long position) throws IllegalArgumentException, com.sun.star.io.IOException {
        pos = (int) position;
    }
}