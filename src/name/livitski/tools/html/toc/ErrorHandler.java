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

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

/**
 * Interrupts processing on errors, displays warnings
 * on {@link System#err}.
 */
public class ErrorHandler implements ErrorListener
{
 public void error(TransformerException exception) throws TransformerException
 {
  throw exception;
 }

 public void fatalError(TransformerException exception)
   throws TransformerException
 {
  throw exception;
 }

 public void warning(TransformerException ex)
   throws TransformerException
 {
  if (debug)
   ex.printStackTrace();
  else
   System.err.println(ex.getLocalizedMessage());
 }

 public ErrorHandler debug(boolean flag)
 {
  debug = flag;
  return this;
 }

 private boolean debug;
}
