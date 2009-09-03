/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.psd;

import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.ImageReader;
import java.io.IOException;
import java.util.Locale;

/**
 * PSDImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDImageReaderSpi.java,v 1.0 Apr 29, 2008 4:49:03 PM haraldk Exp$
 */
public class PSDImageReaderSpi extends ImageReaderSpi {

    /**
     * Creates an PSDImageReaderSpi
     */
    public PSDImageReaderSpi() {
        super(
                "TwelveMonkeys",
                "2.0",
                new String[]{"psd", "PSD"},
                new String[]{"psd"},
                new String[]{
                        "application/vnd.adobe.photoshop", // This one seems official, used in XMP 
                        "image/x-psd", "application/x-photoshop", "image/x-photoshop"
                },
                "com.twelvemkonkeys.imageio.plugins.psd.PSDImageReader",
                STANDARD_INPUT_TYPE,
//                new String[]{"com.twelvemkonkeys.imageio.plugins.psd.PSDImageWriterSpi"},
                null,
                true, null, null, null, null,
                true, null, null, null, null
        );
    }

    public boolean canDecodeInput(Object pSource) throws IOException {
        if (!(pSource instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) pSource;

        stream.mark();
        try {
            return stream.readInt() == PSD.SIGNATURE_8BPS;
            // TODO: Test more of the header, see PSDImageReader#readHeader
        }
        finally {
            stream.reset();
        }
    }

    public ImageReader createReaderInstance(Object pExtension) throws IOException {
        return new PSDImageReader(this);
    }

    public String getDescription(Locale pLocale) {
        return "Adobe Photoshop Document (PSD) image reader";
    }
}