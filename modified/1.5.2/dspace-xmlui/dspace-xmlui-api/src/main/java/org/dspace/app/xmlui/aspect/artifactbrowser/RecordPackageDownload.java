package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.util.Map;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UsageEvent;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

/**
 * Cocoon action that records a jorum content package download.  The download is
 * recorded by firing an event UsageEvent using the dspace item id and a
 * bitstream id of 0 
 */
public class RecordPackageDownload extends AbstractAction
{
    /*
     * (non-Javadoc)
     * @see org.apache.cocoon.acting.Action#act(org.apache.cocoon.environment.Redirector, org.apache.cocoon.environment.SourceResolver, java.util.Map, java.lang.String, org.apache.avalon.framework.parameters.Parameters)
     */
    @SuppressWarnings("unchecked")
    public Map act(Redirector redirector,
            SourceResolver resolver,
            Map objectModel,
            String source,
            Parameters parameters) throws Exception
    {
        Context context = ContextUtil.obtainContext(objectModel);
        DSpaceObject dso = null; 
        String itemId = null;
        String handle = null;
        
        try
        {
            itemId = parameters.getParameter("item-id");
        }
        catch(ParameterException ex){}
        
        if(itemId != null)
        {
            // item id has been given so find the dspace item
            dso = DSpaceObject.find(
                    context, Constants.ITEM, Integer.parseInt(itemId));
        }
        else
        {
            try
            { 
                // handle has been given so find the dspace
                // item using the HandleManager
                handle = parameters.getParameter("handle");
                dso = HandleManager.resolveToObject(context, handle);
            }
            catch(ParameterException ex){}
           
        }
        
        if(dso != null && dso instanceof Item)
        {
            // DSpace item has been found, fire event
            new UsageEvent().fire((Request) ObjectModelHelper.getRequest(objectModel), 
                    context,
                    UsageEvent.VIEW,
                    Constants.BUNDLE,
                    ((Item)dso).getID());
        }
        
        return null;
    }
}
