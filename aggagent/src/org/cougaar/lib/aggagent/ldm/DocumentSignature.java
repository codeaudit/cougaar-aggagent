
package org.cougaar.lib.aggagent.ldm;



import java.util.Collection;
import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;


//
// PLACEHOLDER FOR FUTURE IMPLEMENTATION
// -- THIN ENCAPSULATION FOR AGG AGENT BLACKBOARD CONTENTS
//

public class DocumentSignature
{
     public DocumentSignature(Document doc) {
          System.out.println("entered doc sig, doc=" + doc + " children=" + doc.getChildNodes().getLength() );

          int nchildren = doc.getChildNodes().getLength();

          System.out.println("[DocumentSignature]" + nchildren );


     }
}

