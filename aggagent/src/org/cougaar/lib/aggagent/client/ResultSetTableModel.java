/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.lib.aggagent.client;

import java.util.Iterator;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.cougaar.lib.aggagent.query.AggregationResultSet;
import org.cougaar.lib.aggagent.query.ResultSetDataAtom;
import org.cougaar.lib.aggagent.query.UpdateListener;

/**
 * This class provides a table model of an AggregationResultSet
 */
public class ResultSetTableModel extends AbstractTableModel
{
  private Vector idHeaders = new Vector();
  private Vector valueHeaders = new Vector();
  private Vector dataAtoms = new Vector();
  private AggregationResultSet observedResultSet = null;
  private UpdateListener resultSetListener = null;

  public ResultSetTableModel()
  {
    resultSetListener = new UpdateListener() {
        public void objectAdded(Object sourceObject)
        {
          objectChanged(sourceObject);
        };
        public void objectRemoved(Object sourceObject)
        {
          /* not my problem (clear model?) */
        };
        public void objectChanged(Object sourceObject)
        {
          updateInternalRepOfResultSet(observedResultSet);
        }
      };
  }

  public void setResultSet(final AggregationResultSet rs)
  {
    if (observedResultSet != null)
    {
      observedResultSet.removeUpdateListener(resultSetListener);
    }
    observedResultSet = rs;
    if (observedResultSet != null)
    {
      observedResultSet.addUpdateListener(resultSetListener);
    }
    updateInternalRepOfResultSet(observedResultSet);
  }

  public AggregationResultSet getResultSet()
  {
    return observedResultSet;
  }

  private void updateInternalRepOfResultSet(AggregationResultSet rs)
  {
    Vector oldIdHeaders = (Vector)idHeaders.clone();
    Vector oldValueHeaders = (Vector)valueHeaders.clone();
    idHeaders.clear();
    valueHeaders.clear();
    dataAtoms.clear();
    if (rs != null)
    {
      Iterator atoms = rs.getAllAtoms();
      boolean first = true;
      while (atoms.hasNext())
      {
        ResultSetDataAtom da = (ResultSetDataAtom)atoms.next();

        if (first)
        {
          first = false;
          for (Iterator i = da.getIdentifierNames(); i.hasNext();)
          {
            idHeaders.add(i.next());
          }
          for (Iterator i = da.getValueNames(); i.hasNext();)
          {
            valueHeaders.add(i.next());
          }
        }

        dataAtoms.add(da);
      }
    }

    // fire proper table change event
    if ((idHeaders.equals(oldIdHeaders)) &&
        (valueHeaders.equals(oldValueHeaders)))
    {
      fireTableDataChanged();
    }
    else
    {
      fireTableStructureChanged();
    }
  }

  public int getRowCount()
  {
    return dataAtoms.size();
  }

  public int getColumnCount()
  {
    return idHeaders.size() + valueHeaders.size();
  }

  public String getColumnName(int column)
  {
    String name = null;
    int idCount = idHeaders.size();
    if (column < idCount)
    {
      name = idHeaders.elementAt(column).toString();
    }
    else
    {
      name = valueHeaders.elementAt(column - idCount).toString();
    }

    return name;
  }

  public Object getValueAt(int row, int column)
  {
    Object value = null;
    ResultSetDataAtom da = (ResultSetDataAtom)dataAtoms.elementAt(row);

    int idCount = idHeaders.size();
    if (column < idCount)
    {
      value = da.getIdentifier(idHeaders.elementAt(column));
    }
    else
    {
      value = da.getValue(valueHeaders.elementAt(column - idCount));
    }

    return value;
  }
}
