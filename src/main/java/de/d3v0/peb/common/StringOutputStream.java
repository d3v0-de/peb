package de.d3v0.peb.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class StringOutputStream extends OutputStream
{

    StringBuilder mBuf = new StringBuilder();
    int pos = 0;

    @Override
    public void write(int word) throws IOException
    {
        mBuf.append((char) word);
    }

    public InputStream getInputStream()
    {
        return new ByteArrayInputStream(getString().getBytes(StandardCharsets.UTF_8));
    }

    public String getString() {
        return mBuf.toString();
    }
}
