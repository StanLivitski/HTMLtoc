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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Command-line tool that
 * processes a file pointed to by the first argument, rendering
 * output to the {@link System#out standard output stream}. Error
 * messages are printed on the {@link System#err standard error stream}.
 * Set the {@link #DEBUG_PROPERTY debug} system property
 * to <code>true</code> to see stack traces of error when they occur.
 * There must be one and only one argument to this command, and
 * it must point to an existing file.
 * Set the <code>name.livitski.tools.html.toc.encoding</code> system
 * property to change the encoding in which the files are read and written.
 * If not set, the system default encoding is used.
 */
public class ProcessFile implements Runnable
{
 /**
  * Entry point into the processor. Returns on success,
  * exits with a non-zero status on failure.
  * @see ProcessFile
  */
 public static void main(String[] args)
 {
  ProcessFile job = new ProcessFile().withArguments(args);
  Status status = job.getStatus();
  if (null == status)
  {
   job.run();
   status = job.getStatus();
   if (null == status)
   {
    System.err.println("Internal error in XML TOC processor, the process did not complete.");
    status = Status.INTERNAL;
   }
  }
  if (Status.OK != status)
   System.exit(status.getCode());
 }

 public void run()
 {
  if (null != status)
   return;
  String encoding = Transformer.defaultEncoding();
  FileInputStream fileInput = null;
  StreamResult target = null;
  try
  {
   fileInput = new FileInputStream(file);
   StreamSource source = new StreamSource(
     new InputStreamReader(new BufferedInputStream(fileInput), encoding));
   target = new StreamResult(
     new OutputStreamWriter(System.out, encoding)
     {
      @Override
      public void close() throws IOException
      {
       flush();
       System.out.flush();
      }
     }
   );
   Transformer processor = new Transformer();
   processor.setErrorListener(new ErrorHandler().debug(Boolean.getBoolean(DEBUG_PROPERTY)));
   processor.transform(source, target);
   status = Status.OK;
  }
  catch (TransformerException e)
  {
   String legend;
   Throwable report = e;
   if (e instanceof TransformerConfigurationException)
   {
    legend = "Internal error";
    status = Status.INTERNAL;
   }
   else if (e.getCause() instanceof IOException)
   {
    legend = "Input/output error";
    report = e.getCause();
    status = Status.IOERR;
   }
   else
   {
    legend = "Data error";
    status = Status.SYNTAX;
   }
   reportProcessingError(legend, report);
  }
  catch (UnsupportedEncodingException ex)
  {
   String legend = "Internal error";
   reportProcessingError(legend, ex);
   status = Status.INTERNAL;
  }
  catch (IOException ex)
  {
   String legend = "Input/output error";
   reportProcessingError(legend, ex);
   status = Status.IOERR;
  }
  catch (RuntimeException ex)
  {
   String legend = "Internal error";
   reportProcessingError(legend, ex);
   status = Status.INTERNAL;
  }
  catch (Error err)
  {
   String legend = "System error";
   reportProcessingError(legend, err);
   status = Status.SYSTEM;
  }
  finally
  {
   if (null != target)
   try
   {
     OutputStream stream = target.getOutputStream();
     if (null == stream && null != target.getWriter())
      target.getWriter().close();
     else if (System.out == stream || System.err == stream)
      stream.flush();
     else if (null != stream)
      stream.close();
   }
   catch(IOException ioex)
   {
    if (null == status || Status.OK == status)
    {
     status = Status.IOERR;
     reportProcessingError("Input/output error", ioex);
    }
   }
   if (null != fileInput)
   try
   {
     fileInput.close();
   }
   catch(IOException ioex)
   {
    if (null == status || Status.OK == status)
    {
     status = Status.IOERR;
     reportProcessingError("Input/output error", ioex);
    }
   }
  }
 }

 public Status getStatus()
 {
  return status;
 }

 public ProcessFile withArguments(String[] args)
 {
  if (0 == args.length)
  {
   System.err.println("Please enter location of a file to transform as an argument.");
   status = Status.NOARGS;
   return this;
  }
  if (1 < args.length)
  {
   System.err.println("Cannot process extra argument \"" + args[1] + '"');
   status = Status.EXTRAARGS;
   return this;
  }
  file = new File(args[0]);
  if (!file.exists() || file.isDirectory())
  {
   System.err.println("File \"" + file + "\" does not exist or is a directory");
   status = Status.NOFILE;
   return this;
  }
  return this;
 }

 /**
  * Set the <code>debug</code> system property to <code>true</code> to see
  * the complete stack trace of an error when it occurs.
  */
 public static final String DEBUG_PROPERTY = "debug";

 private void reportProcessingError(String legend, Throwable ex)
 {
  System.err.println(legend + " while processing file \"" + file + "\":");
  if (Boolean.getBoolean(DEBUG_PROPERTY))
   ex.printStackTrace();
  else
   System.err.println(ex.getMessage());
 }

 private File file;
 private Status status;

 /**
  * Exit codes returned from {@link ProcessFile#main(String[]) this class}. 
  */
 public enum Status
 {
  /** Successful completion. */
  OK,
  /** No arguments on the command line. */
  NOARGS,
  /** Extra arguments on the command line. */
  EXTRAARGS,
  /** File on the command line does not exist. */
  NOFILE,
  /** Input/output error. */
  IOERR,
  /** Invalid content. */
  SYNTAX,
  /* TODO: Add error codes here */
  /** Internal error. */
  INTERNAL(-1),
  /** System error. */
  SYSTEM(-2);

  public int getCode()
  {
   return code;
  }

  Status()
  {
   code = ordinal();
  }

  Status(int code)
  {
   this.code = code;
  }

  private int code;
 }
}
