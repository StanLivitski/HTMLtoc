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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.stream.EventFilter;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.OutputKeys;

import name.livitski.tools.xml.staxform.XMLEventProcessor;
import name.livitski.tools.xml.staxform.XMLEventTransformer;
import name.livitski.tools.xml.staxform.helpers.IdentityEventProcessor;
import name.livitski.tools.xml.staxform.helpers.ConditionalEventProcessor;

import static javax.xml.stream.XMLStreamConstants.*;
import static javax.xml.XMLConstants.*;

/**
 * Transforms an XHTML document by finding processing instructions
 * {@link TocPIParser#PI_TARGET targeted at this package} and replacing them with
 * generated table of contents.
 * 
 * @see javax.xml.stream
 */
public class Transformer extends XMLEventTransformer
{
 /**
  * Name of the system property used to choose the character encoding
  * of the transformation. 
  */
 public static final String ENCODING_PROPERTY = "name.livitski.tools.html.toc.encoding";

 public static String defaultEncoding()
 {
  String encoding = System.getProperty(ENCODING_PROPERTY);
  if (null == encoding)
  {
   Charset defaultCharset = Charset.defaultCharset();
   encoding = defaultCharset.name();
  }
  return encoding;
 }

 public Transformer()
 {
  setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
  setFunction(processor());
 }

 @Override
 protected XMLEventProcessor preprocessor()
 {
  return new Preprocessor();
 }

 protected XMLEventProcessor processor()
 {
  Indexer indexer = new Indexer();
  XMLEventProcessor processor = new ConditionalEventProcessor(indexer, indexer)
  {
   @Override
   public void add(XMLEvent event) throws XMLStreamException
   {
    // TODO: replace this patch with proper entity handling
    if (event instanceof EntityReference && "copy".equals(((EntityReference)event).getName()))
     super.add(getXMLEventFactory().createCharacters("\u00a9"));
    else
     super.add(event);
   }
  };
  return processor;
 }

 protected TocPIParser piParser()
 {
  if (null == piParser)
   piParser = new TocPIParser();
  return piParser;
 }

 protected static String describeLocation(Location location)
 {
  String legend = "at offset " + location.getCharacterOffset()
         + ", line " + location.getLineNumber()
         + ", column " + location.getColumnNumber();
  return legend;
 }

 protected static String describeEvent(XMLEvent event)
 {
  switch (event.getEventType())
  {
  case START_ELEMENT:
  {
   StartElement element = (StartElement)event;
   return "element <" + element.getName() +'>';
  }
  case PROCESSING_INSTRUCTION:
  {
   ProcessingInstruction element = (ProcessingInstruction)event;
   return "processing instruction <?" + element.getTarget()
   + ' ' + element.getData() + "?>";
  }
  default:
   return event.toString();
  }
 }

 protected static final QName ID_ATTR_QNAME = new QName("id");

 protected class Preprocessor extends XMLHeaderProcessor
 {
  @Override
  public void add(XMLEvent event) throws XMLStreamException
  {
   if (!(event instanceof DTD))
    super.add(event);
  }
 }

 protected class Indexer extends IdentityEventProcessor implements EventFilter
 {
  @Override
  public void reset() throws XMLStreamException
  {
   super.reset();
   state = State.ROOT;
   endContext();
   deferred.clear();
   lastId = 0;
   idBuf.replace(3, 9, "000000");
   piData = null;
   formatter = null;
  }

  @Override
  public void add(XMLEvent event) throws XMLStreamException
  {
   if (event instanceof ProcessingInstruction)
   {
    if (null == piData)
     piData = parsePIEvent((ProcessingInstruction)event);
   }
   else
    piData = null;
   if (null != piData)
   {
    if (piData.isOpening())
    {
     formatter = TocFormatter.forPI(piData, event.getLocation());
     formatter.setXMLEventFactory(getXMLEventFactory());
    }
    if (piData.isClosing())
     super.add(getXMLEventFactory().createCharacters("\n"));
   }
   else if (state == State.INDEXED)
    index(event);
   else if (state == State.ROOT)
   {
    addDTD((StartElement)event);
    super.add(event);
    state = State.PASSTHROUGH;
   }
  }

  public boolean accept(XMLEvent event)
  {
    switch (state)
    {
    case ROOT:
     if (event instanceof ProcessingInstruction
       && !TocPIParser.isIgnoredPI((ProcessingInstruction)event))
      throw new IllegalStateException("Processing instructions <?" + TocPIParser.PI_TARGET
        + "?> cannot be placed outside the root element.");
     else if (event instanceof StartElement)
      break;
     else
      return false;
    case PASSTHROUGH:
     if (!(event instanceof ProcessingInstruction))
      return false;
     piData = filterPIEvent((ProcessingInstruction)event);
     if (null == piData)
      return false;
     if (!piData.isClosing())
     {
      enterContext(event);
      state = State.PLACEHOLDER;
     }
     else
      state = State.INDEXED;
     break;
    case PLACEHOLDER:
     if (!(event instanceof ProcessingInstruction))
     {
      trackContext(event);
      break;
     }
     piData = filterPIEvent((ProcessingInstruction)event);
     if (null == piData)
      break;
     if (piData.isOpening())
      throw new IllegalStateException("Processing instruction <?" + TocPIParser.PI_TARGET
        + "?> cannot be nested. Nesting instruction began" + describeLocation(origin.getLocation()));
     else if (!context.isEmpty())
     {
      StartElement open = context.get(0);
      throw new IllegalStateException("Unclosed element <" + open.getName() + "> "
          + describeLocation(open.getLocation())
          + " within placeholder XML for processing instruction <?" + TocPIParser.PI_TARGET
          + "?>");
     }
     else
     {
      endContext();
      state = State.INDEXED;
     }
     break;
    case INDEXED:
     piData = null;
     break;
    default:
     throw new RuntimeException("Unexpected transformer state: " + state);
    }
    return true;
  }

  protected void addDTD(StartElement event) throws XMLStreamException
  {
   QName qName = ((StartElement)event).getName();
   String name = qName.getPrefix();
   if (null == name)
    name = "";
   else if (0 < name.length())
    name += ':';
   name += qName.getLocalPart();
   XMLEventFactory eventFactory = getXMLEventFactory();
   DTD dtdEvent = eventFactory.createDTD("<!DOCTYPE " + name + '>');
   super.add(dtdEvent);
   super.add(eventFactory.createCharacters("\n"));
  }

  protected void index(XMLEvent event) throws XMLStreamException
  {
   final Location location = event.getLocation();
   try
   {
    if (event instanceof EndDocument)
    {
     if (null != context)
      trackContext(event);
     if (null != formatter)
     {
      formatter.end();
      conveyFormatted();
     }
     for (XMLEvent resumed : deferred)
      super.add(resumed);
     super.add(event);
    }
    else if (event instanceof EndElement && null != context && context.isEmpty())
    {
     EndElement close = (EndElement)event;
     if (!((StartElement)origin).getName().equals(close.getName()))
      throw new IllegalStateException("Unclosed " + describeEvent(origin) + ' '
          + describeLocation(origin.getLocation()));
     endContext();
     if (null != formatter)
     {
      formatter.closeItem(event);
      conveyFormatted();
     }
     deferred.add(event);
    }
    else if (null == context)
    {
     String id = null;
     if (null != formatter && formatter.accept(event))
     {
      event = assignIdToElement((StartElement)event);
      id = ((StartElement)event).getAttributeByName(ID_ATTR_QNAME).getValue();
     }
     deferred.add(event);
     if (null != id)
     {
      XMLEventFactory eventFactory = getXMLEventFactory();
      Set<Attribute> attrs = Collections.singleton(
	eventFactory.createAttribute("name", id)
      );
      deferred.add(eventFactory.createStartElement(
	DEFAULT_NS_PREFIX,
	NULL_NS_URI,
	"a",
	attrs.iterator(),
	Collections.EMPTY_SET.iterator()));
      deferred.add(eventFactory.createCharacters(" "));
      deferred.add(eventFactory.createEndElement(DEFAULT_NS_PREFIX, NULL_NS_URI, "a"));
      if (null != formatter)
      {
       formatter.openItem(event, id);
       conveyFormatted();
      }
      enterContext(event);
     }
    }
    else // null != context
    {
     trackContext(event);
     deferred.add(event);
     if (null != formatter)
     {
      formatter.addContent(event);
      conveyFormatted();
     }
    }
   }
   catch (RuntimeException error)
   {
    throw new XMLStreamException(error.getLocalizedMessage(), location, error);
   }
  }

  protected void conveyFormatted() throws XMLStreamException
  {
   while (formatter.hasNext())
   {
    XMLEvent inter = formatter.next();
    super.add(inter);
   }
  }

  @SuppressWarnings("unchecked")
  protected StartElement assignIdToElement(StartElement element)
  {
   List<Attribute> attrs = new ArrayList<Attribute>();
   Attribute idAttr = null;
   for (Iterator<Attribute> i = element.getAttributes(); i.hasNext();)
   {
    Attribute attr = i.next();
    if (ID_ATTR_QNAME.equals(attr.getName()))
     idAttr = attr;
    attrs.add(attr);
   }
   if (null == idAttr)
   {
    String id = Integer.toString(++lastId);
    int len = id.length();
    if (6 < len)
     throw new IndexOutOfBoundsException("Too many TOC entries: " + id + ", cannot allocate an id");
    id = idBuf.replace(9 - len, 9, id).toString();
    XMLEventFactory eventFactory = getXMLEventFactory();
    idAttr = eventFactory.createAttribute(ID_ATTR_QNAME, id);
    attrs.add(idAttr);
    eventFactory.setLocation(element.getLocation());
    element = eventFactory.createStartElement(
     element.getName(),
     attrs.iterator(),
     element.getNamespaces());
    eventFactory.setLocation(null);
   }
   return element;
  }

  // TODO: extract element context tracking into a (library) event processor class
  protected void enterContext(XMLEvent event)
  {
   if (null != context)
    throw new IllegalStateException(
      "Cannot create nested context for " +  describeEvent(event)
      + " within a context of " + describeEvent(origin) + ' '
      + describeLocation(origin.getLocation()));
   origin = event;
   context = new LinkedList<StartElement>();
  }

  protected void trackContext(XMLEvent event)
  {
   switch (event.getEventType())
   {
   case START_ELEMENT:
    context.add(0, (StartElement)event);
    break;
   case END_DOCUMENT:
   case END_ELEMENT:
    if (!context.isEmpty())
    {
     StartElement open = context.remove(0);
     EndElement close = event instanceof EndElement ? (EndElement)event : null;
     if (null == close || !open.getName().equals(close.getName()))
      throw new IllegalStateException("Unclosed " + describeEvent(open) + ' '
	      + describeLocation(open.getLocation()));
    }
    else
     throw new IllegalStateException("Unclosed " + describeEvent(origin) + ' '
       + describeLocation(origin.getLocation()));
    break;
   case PROCESSING_INSTRUCTION:
    if (!TocPIParser.isIgnoredPI((ProcessingInstruction)event))
     throw new IllegalStateException("Processing instructions <?" + TocPIParser.PI_TARGET
       + "?> are not allowed within the context of " + describeEvent(origin));
    break;
   }
  }

  protected void endContext()
  {
   context = null;
   origin = null;
  }

  protected TocPIData parsePIEvent(ProcessingInstruction event)
    throws XMLStreamException
  {
   try
   {
    return piParser().parse(event);
   }
   catch (JAXBException pierr)
   {
    throw new XMLStreamException("Error parsing " + describeEvent(event),
      event.getLocation(), pierr);
   }
  }

  protected TocPIData filterPIEvent(ProcessingInstruction event)
  {
   try
   {
    return piParser().parse(event);
   }
   catch (JAXBException pierr)
   {
    throw new IllegalArgumentException("Error parsing " + describeEvent(event), pierr);
   }
  }

  private State state = State.ROOT;
  private List<StartElement> context;
  private XMLEvent origin;
  private List<XMLEvent> deferred = new ArrayList<XMLEvent>();
  private StringBuilder idBuf = new StringBuilder("toc000000");
  private int lastId = 0;
  private TocFormatter formatter;
  private TocPIData piData;
 }

 protected enum State
 {
  PASSTHROUGH,
  PLACEHOLDER,
  ROOT,
  INDEXED;
 }

 private TocPIParser piParser;
}
