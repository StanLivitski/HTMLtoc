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

import java.util.AbstractList;

/**
 * Provides read-only access to a list of outline items,
 * such as element names.
 */
public class OutlineList extends AbstractList<String>
{
 public OutlineList(String outline)
 {
  elements = outline.trim().split("\\s*,\\s*", -1);
 }

 @Override
 public String get(int index)
 {
  return elements[index];
 }

 @Override
 public int size()
 {
  return elements.length;
 }

 private String[] elements;
}
