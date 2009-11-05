package org.deepfs.fsml.extractors;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import org.basex.build.Builder;
import org.basex.core.Main;
import org.basex.util.Token;
import static org.basex.build.fs.FSText.*;

/**
 * JPG meta data extractor.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Christian Gruen
 */
public final class JPGExtractor extends EXIFExtractor {
  @Override
  public void extract(final Builder build, final File f) throws IOException {
    final BufferedInputStream in =
      new BufferedInputStream(new FileInputStream(f));

    // check if the header is valid
    for(final int i : HEADERJPG) if(in.read() != i) return;

    // find image dimensions
    while(true) {
      int b = in.read();
      if(b == -1) return;
      if(b != 0xFF) continue;
      b = in.read();
      if(b >= 0xC0 && b <= 0xC3) break;
      int skip = (in.read() << 8) + in.read() - 2;

      try {
        if(b == 0xE1) skip = scanEXIF(in, skip, f.getName());
      } catch(final IOException ex) {
        Main.debug(f + ": " + ex.getMessage());
        exif.clear();
      }
      skip(in, skip);
    }
    skip(in, 3);

    // extract image dimensions
    final int h = (in.read() << 8) + in.read();
    final int w = (in.read() << 8) + in.read();

    in.close();

    // open image tag
    build.startElem(IMAGE, atts.set(TYPE, TYPEJPG));

    build.nodeAndText(WIDTH, atts.reset(), Token.token(w));
    build.nodeAndText(HEIGHT, atts, Token.token(h));

    if(!exif.isEmpty()) {
      build.startElem(EXIF, atts.reset());
      final Iterator<byte[]> it = exif.iterator();
      while(it.hasNext()) build.nodeAndText(it.next(), atts, it.next());
      build.endElem(EXIF);
      exif.clear();
    }

    build.endElem(IMAGE);
  }
}