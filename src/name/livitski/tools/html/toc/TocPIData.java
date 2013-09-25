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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Encapsulates the parsed content of the processing instructions
 * for this package. 
 */
@XmlRootElement(name="toc-pi", namespace="")
public class TocPIData
{
 /**
  * Argumentless constructor required by JAXB.
  */
 public TocPIData()
 {
 }

 public String getVersion()
 {
  return version;
 }

 @XmlAttribute(required=true)
 public void setVersion(String version)
 {
  this.version = version;
 }

 /**
  * Lists the elements that generate TOC entries. A tag's position
  * on the list determines its level in the table structure. The
  * list elements must be unique and cannot be empty.
  * @return the outline list of elements
  * @see OutlineList
  */
 public String getOutline()
 {
  return outline;
 }

 @XmlAttribute(required=false)
 public void setOutline(String outline)
 {
  this.outline = outline;
 }

 /**
  * Lists the elements generated to group TOC entries at
  * each level of the outline. This list may contain fewer
  * elements than {@link #getOutline() the outline}, and some
  * elements may be empty. Missing or empty elements will
  * be replaced by {@link TocFormatter#DEFAULT_BLOCK_WRAPPER}
  * in the TOC. Tag names on the list may be followed by a dot '.'
  * and a CSS class name to apply to generated elements.
  * @see #getOutline() 
  * @see OutlineList
  */
 public String getBlocktags()
 {
  return blocktags;
 }

 @XmlAttribute(required=false)
 public void setBlocktags(String blocktags)
 {
  this.blocktags = blocktags;
 }

 /**
  * Lists the elements wrapping individual TOC entries at
  * each level of the outline. This list may contain fewer
  * items than {@link #getOutline() the outline}, and some
  * items may be empty. Missing or empty items will
  * be replaced with {@link TocFormatter#DEFAULT_LINE_WRAPPER}
  * in the TOC. Tag names on the list may be followed by a dot '.'
  * and a CSS class name to apply to generated HTML elements.
  * @see #getOutline() 
  * @see OutlineList
  */
 public String getLinetags()
 {
  return linetags;
 }

 @XmlAttribute(required=false)
 public void setLinetags(String linetags)
 {
  this.linetags = linetags;
 }

 public boolean isOpening()
 {
  return opening;
 }

 @XmlTransient
 public void setOpening(boolean opening)
 {
  this.opening = opening;
 }

 public boolean isClosing()
 {
  return closing;
 }

 @XmlTransient
 public void setClosing(boolean closing)
 {
  this.closing = closing;
 }

 public boolean isEmpty()
 {
  return opening && closing;
 }

 private String version;
 private String outline, blocktags, linetags;
 private boolean opening, closing;
}
