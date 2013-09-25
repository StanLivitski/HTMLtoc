/**
 *  This file is part of HTMLtoc.
 *  Copyright © 2013 Konstantin Livitski
 *
 *  HTMLtoc is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package name.livitski.tools.html.toc;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.events.ProcessingInstruction;

/**
 * Parses the {@link ProcessingInstruction XML events} for
 * the tool's {@link TocPIParser#PI_TARGET processing instructions}
 * and converts them into {@link TocPIData data objects}. 
 */
public class TocPIParser
{
 /**
  * The target of processing instructions handled by this processor.
  * Set to this class's package name.
  */
 public static final String PI_TARGET = TocPIParser.class.getPackage().getName();

 /**
  * Tells whether a processing instruction will be ignored by this
  * parser.
  * @param pi the processing instruction to test
  * @return whether or not the instruction is ignored
  * @see #PI_TARGET
  */
 public static boolean isIgnoredPI(ProcessingInstruction pi)
 {
  return !PI_TARGET.equals(pi.getTarget());
 }

 /**
  * Parses an {@link ProcessingInstruction XML event} of a
  * processing instruction.
  * @param pi the event to parse
  * @return the data from the processing instruction or <code>null</code>
  * if the event is {@link #isIgnoredPI(ProcessingInstruction) ignored}
  * by this parser
  */
 public TocPIData parse(ProcessingInstruction pi) throws JAXBException
 {
  if (isIgnoredPI(pi))
   return null;
  String rawData = pi.getData();
  rawData = null == rawData ? "" : rawData.trim();
  boolean closing = rawData.endsWith("/");
  if (closing)
   rawData = rawData.substring(0, rawData.length() - 1).trim();
  TocPIData data;
  if (0 < rawData.length())
   data = parseAttrs(rawData);
  else if (closing)
   data = new TocPIData();
  else
   throw new JAXBException("Processing instruction contains no data");
  data.setClosing(closing);
  return data;
 }

 protected TocPIData parseAttrs(String raw) throws JAXBException
 {
  String dummy = '<' + XML_NAME + ' ' + raw + " />";
  TocPIData data = (TocPIData)unmarshaller().unmarshal(new StringReader(dummy));
  data.setOpening(true);
  String version = data.getVersion();
  if (null == version)
   throw new JAXBException("Version attribute missing for <?" + PI_TARGET + "?>");
  if (!"1.0".equals(version))
   throw new JAXBException("Unsupported version \"" + version + "\" for <?" + PI_TARGET + "?>");
  return data;
 }

 protected Unmarshaller unmarshaller() throws JAXBException
 {
  if (null == unmarshaller)
  {
   JAXBContext context = JAXBContext.newInstance(TocPIData.class);
   unmarshaller = context.createUnmarshaller();
  }
  return unmarshaller;
 }

 protected static final String XML_NAME =
  ((XmlRootElement)TocPIData.class.getAnnotation(XmlRootElement.class)).name();
 private Unmarshaller unmarshaller;
}
