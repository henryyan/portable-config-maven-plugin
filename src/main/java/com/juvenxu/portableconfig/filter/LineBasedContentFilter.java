package com.juvenxu.portableconfig.filter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.juvenxu.portableconfig.ContentFilter;
import com.juvenxu.portableconfig.model.Replace;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * @author juven
 */
public abstract class LineBasedContentFilter extends AbstractLogEnabled implements ContentFilter
{

  protected abstract String filterLine(String line, List<Replace> replaces);

  @Override
  public void filter(InputStream fileIS, OutputStream tmpOS, List<Replace> replaces) throws IOException
  {
    this.filter(new InputStreamReader(fileIS), new OutputStreamWriter(tmpOS), replaces);
  }

  protected void filter(Reader reader, Writer writer, List<Replace> replaces) throws IOException
  {
    BufferedReader bufferedReader = new BufferedReader(reader);
    BufferedWriter bufferedWriter = new BufferedWriter(writer);

    Set<String> replacedKeys = new HashSet<String>();

    while (bufferedReader.ready())
    {
      String line = bufferedReader.readLine();

      if (line == null)
      {
        break;
      }

      if (StringUtils.isBlank(line))
      {
        continue;
      }

      String filterLine = filterLine(line, replaces);
      bufferedWriter.write(filterLine);
      bufferedWriter.newLine();

      // add to replaced pool
      String key = StringUtils.substringBefore(line, "=");
      key = StringUtils.trim(key);
      replacedKeys.add(key);
    }

    // add new properties
    int addCount = 0;
    for (Replace replace : replaces) {
      String key = replace.getKey();
      if (!replacedKeys.contains(key)) {
        if (!replace.isAddNoExist()) {
          getLogger().info("Ignore new property: " + key);
          continue;
        }

        if (addCount == 0) {
          bufferedWriter.newLine();
          bufferedWriter.write("# added by replace plugin at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
          bufferedWriter.newLine();
        }

        String filterLine = key + "=" + replace.getValue();
        getLogger().info("Add new property: " + key);
        bufferedWriter.write(filterLine);
        bufferedWriter.newLine();
        addCount++;
      }
    }

    bufferedWriter.flush();
  }
}
