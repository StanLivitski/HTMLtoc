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

import static javax.xml.XMLConstants.DEFAULT_NS_PREFIX;
import static javax.xml.XMLConstants.NULL_NS_URI;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.EventFilter;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import name.livitski.tools.xml.staxform.helpers.IdentityEventProcessor;

/**
 * Detects relevant items in a stream of XML events and formats them for
 * inclusion in the table of contents (TOC). The items to include in the
 * TOC are selected using the {@link TocPIData formatter's processing instruction}.
 * Groups the TOC items into levels according to the configured outline.  
 */
public class TocFormatter extends IdentityEventProcessor implements EventFilter
{
 /**
  * Creates a formatter from settings within the processing instruction.
  * @param pi the processing instruction with parameters of the new formatter
  * @return the new formatter
  */
 public static TocFormatter forPI(TocPIData pi, Location location) throws XMLStreamException
 {
  String attrValue = pi.getOutline();
  if (null == attrValue || 0 == attrValue.length())
   throw new XMLStreamException(
     "Invalid empty outline in <?" + TocPIParser.PI_TARGET + "?>",
     location);
  OutlineList outline = new OutlineList(attrValue);
  attrValue = pi.getBlocktags();
  if (null == attrValue)
   attrValue = "";
  Iterator<String> blocktags = new OutlineList(attrValue).iterator();
  attrValue = pi.getLinetags();
  if (null == attrValue)
   attrValue = "";
  Iterator<String> linetags = new OutlineList(attrValue).iterator();
  TocFormatter formatter = new TocFormatter();
  formatter.levels = new LinkedHashMap<QName, Level>();
  for (int i = 0; outline.size() > i; i++)
  {
   String indexable = outline.get(i);
   if (null == indexable || 0 == indexable.length())
    throw new XMLStreamException(
      "Outline element #" + i + " is empty in <?" + TocPIParser.PI_TARGET + "?>",
      location);
   String blockSpec = blocktags.hasNext() ? blocktags.next() : "";
   if (0 == blockSpec.length())
    blockSpec = DEFAULT_BLOCK_WRAPPER;
   String lineSpec = linetags.hasNext() ? linetags.next() : "";
   if (0 == lineSpec.length())
    lineSpec = DEFAULT_LINE_WRAPPER;
   Level level = formatter.new Level(i, indexable, blockSpec, lineSpec);
   Level conflicting = formatter.levels.put(level.getIndexableElementName(), level);
   if (null != conflicting)
    throw new XMLStreamException(
      "Outline element #" + i + " <" + indexable 
      + "> is the same as element #" + conflicting.getIndex()
      + " in <?" + TocPIParser.PI_TARGET + "?>. Outline elements must be unique.",
      location);
  }
  formatter.levelsIndex = formatter.levels.values().toArray(new Level[formatter.levels.size()]);
  return formatter;
 }

 /**
  * Detects elements to be {@link #addContent added to the TOC}.
  */
 public boolean accept(XMLEvent event)
 {
  return event instanceof StartElement
    && levels.containsKey(((StartElement)event).getName());
 }

 /**
  * Opens a TOC item.
  * A call to this method is likely to generate 
  * {@link #hasNext() outgoing XML events} within the formatter.
  * @param start the event that begins a TOC item in the document.
  * Currently, this must be a {@link StartElement} event
  * @param id the marker assigned to the item for references by TOC
  * @throws XMLStreamException
  */
 public void openItem(XMLEvent start, String id) 
 	throws XMLStreamException
 {
  if (null != openItem)
   throw new XMLStreamException(
     "TOC item " + Transformer.describeEvent(start)
     + " is improperly nested within another TOC item " + Transformer.describeEvent(openItem)
     + " that began " + Transformer.describeLocation(openItem.getLocation()),
     start.getLocation());
  if (!(start instanceof StartElement))
   throw new XMLStreamException(
     "Unexpected event type " + Transformer.describeEvent(start) + " opening a TOC item",
     start.getLocation());
  openItem = (StartElement)start;
  Level level = levels.get(openItem.getName());
  if (null == level)
   throw new XMLStreamException(
     "TOC item " + Transformer.describeEvent(start)
     + " is not included in the outline",
     start.getLocation());
  jumpToLevel(level);
  StartElement startLineElement = level.startLineElement();
  if (null != startLineElement)
   super.add(startLineElement);
  XMLEventFactory eventFactory = getXMLEventFactory();
  Set<Attribute> attrs = Collections.singleton(
	eventFactory.createAttribute("href", '#' + id)
  );
  super.add(
	eventFactory.createStartElement(
	  DEFAULT_NS_PREFIX,
	  NULL_NS_URI,
	  "a",
	  attrs.iterator(),
	  Collections.EMPTY_SET.iterator()));
 }

 /**
  * Closes a TOC item.
  * A call to this method is likely to generate 
  * {@link #hasNext() outgoing XML events} within the formatter.
  * @param end the event that ends a TOC item in the document.
  * Currently, this must be an {@link EndElement} event
  * @throws XMLStreamException
  */
 public void closeItem(XMLEvent end) 
 	throws XMLStreamException
 {
  if (null == openItem)
   throw new XMLStreamException(
     "Attempted to close a TOC item " + Transformer.describeLocation(end.getLocation())
     + " that was never opened",
     end.getLocation());
  if (!(end instanceof EndElement))
    throw new XMLStreamException(
      "Unexpected event type " + Transformer.describeEvent(end)
      + " closing the TOC item " + Transformer.describeEvent(openItem)
      + ' ' + Transformer.describeLocation(openItem.getLocation()),
      end.getLocation());
  if (!((EndElement)end).getName().equals(openItem.getName()))
   throw new XMLStreamException(
     "Closing event " + Transformer.describeEvent(end)
     + " does not match the opening of the TOC item " + Transformer.describeEvent(openItem)
     + ' ' + Transformer.describeLocation(openItem.getLocation()),
     end.getLocation());
  
  super.add(getXMLEventFactory().createEndElement(DEFAULT_NS_PREFIX, NULL_NS_URI, "a"));
  EndElement endLineElement = atLevel.endLineElement();
  if (null != endLineElement)
   super.add(endLineElement);
  addEOL();

  openItem = null;
 }

 /**
  * Adds XML content to the current TOC item.
  * A call to this method is likely to generate 
  * {@link #hasNext() outgoing XML events} within the formatter.
  * @param content
  * @throws XMLStreamException
  */
 public void addContent(XMLEvent content) 
 	throws XMLStreamException
 {
  if (null == openItem)
   throw new XMLStreamException(
     "TOC content " + Transformer.describeEvent(content)
     + " is not expected outside of a TOC item",
     content.getLocation());
  if (content instanceof Characters && !((Characters)content).isIgnorableWhiteSpace())
   super.add(content);
 }

 /**
  * Closes out the TOC produced by this formatter.
  * A call to this method is likely to generate 
  * {@link #hasNext() outgoing XML events} within the formatter.
  * @throws XMLStreamException
  */
 public void end() throws XMLStreamException
 {
  if (null != openItem)
   throw new XMLStreamException(
     "TOC item " + Transformer.describeEvent(openItem)
     + " has never been closed.", openItem.getLocation());
  jumpToLevel(null);
 }

 @Override
 public void reset() throws XMLStreamException
 {
  // TODO reset this object's state
  atLevel = null;
  super.reset();
 }

 public XMLEventFactory getXMLEventFactory()
 {
  if (null == xmlEventFactory)
   xmlEventFactory = XMLEventFactory.newFactory();
  return xmlEventFactory;
 }

 public void setXMLEventFactory(XMLEventFactory xMLEventFactory)
 {
  this.xmlEventFactory = xMLEventFactory;
 }

 public static String CLASS_DELIMITER = "\\s*\\.\\s*";  
 public static QName CLASS_ATTR_QNAME = new QName("class");  

 public static String DEFAULT_BLOCK_WRAPPER = "";  
 public static String DEFAULT_LINE_WRAPPER = "div";  

 protected void addEOL() throws XMLStreamException
 {
  super.add(getXMLEventFactory().createCharacters("\n"));
 }

 protected void jumpToLevel(Level level) throws XMLStreamException
 {
  int atIndex = null == atLevel ? -1 : atLevel.getIndex();
  int index = null == level ? -1 : level.getIndex();
  if (index > atIndex)
  {
   while (index > atIndex++)
   {
    StartElement startElement = levelsIndex[atIndex].startBlockElement();
    if (null != startElement)
    {
     super.add(startElement);
     addEOL();
    }
   }
  }
  else if (index < atIndex)
  {
   // TODO close out blocks
   while (index < atIndex)
   {
    EndElement endElement = levelsIndex[atIndex--].endBlockElement();
    if (null != endElement)
    {
     super.add(endElement);
     addEOL();
    }
   }
  }
  atLevel = level;
 }

 protected class Level
 {
  public QName getIndexableElementName()
  {
   return indexable;
  }

  public int getIndex()
  {
   return index;
  }

  public StartElement startBlockElement()
  {
   if (null == blockWrap)
    return null;
   XMLEventFactory factory = getXMLEventFactory();
   Set<Attribute> attributes;
   if (null == blockClass)
    attributes = Collections.emptySet();
   else
    attributes = Collections.singleton(factory.createAttribute(CLASS_ATTR_QNAME, blockClass));
   return factory.createStartElement(blockWrap, attributes.iterator(), null);
  }

  public EndElement endBlockElement()
  {
   return null == blockWrap ? null : getXMLEventFactory().createEndElement(blockWrap, null);
  }

  public StartElement startLineElement()
  {
   if (null == lineWrap)
    return null;
   XMLEventFactory factory = getXMLEventFactory();
   Set<Attribute> attributes;
   if (null == lineClass)
    attributes = Collections.emptySet();
   else
    attributes = Collections.singleton(factory.createAttribute(CLASS_ATTR_QNAME, lineClass));
   return factory.createStartElement(lineWrap, attributes.iterator(), null);
  }

  public EndElement endLineElement()
  {
   return null == lineWrap ? null : getXMLEventFactory().createEndElement(lineWrap, null);
  }

  public Level(int index, String indexable, String blockSpec, String lineSpec)
  {
   this.index = index;
   this.indexable = new QName(indexable);
   String[] parts = blockSpec.split(CLASS_DELIMITER, 2);
   this.blockWrap = 0 == parts[0].length() ? null : new QName(parts[0]);
   this.blockClass = 1 < parts.length ? parts[1] : null;
   parts = lineSpec.split(CLASS_DELIMITER, 2);
   this.lineWrap = 0 == parts[0].length() ? null : new QName(parts[0]);
   this.lineClass = 1 < parts.length ? parts[1] : null;
  }

  private QName indexable, blockWrap, lineWrap;
  private String blockClass, lineClass;
  private int index;
 }

 private Map<QName, Level> levels;
 private Level[] levelsIndex;
 private Level atLevel;
 private StartElement openItem;
 private XMLEventFactory xmlEventFactory;
}
