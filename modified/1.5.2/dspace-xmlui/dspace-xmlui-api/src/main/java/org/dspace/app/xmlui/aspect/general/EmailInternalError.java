package org.dspace.app.xmlui.aspect.general;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.components.notification.Notifying;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;

/**
 * Email exception callstacks to all defined by alert.recipient.  The email
 * template "internal_error" is used.
 */
public class EmailInternalError extends AbstractAction
{
    private static final Logger LOG = Logger.getLogger(EmailInternalError.class);
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    
    /*
     * (non-Javadoc)
     * @see org.apache.cocoon.acting.Action#act(org.apache.cocoon.environment.Redirector, org.apache.cocoon.environment.SourceResolver, java.util.Map, java.lang.String, org.apache.avalon.framework.parameters.Parameters)
     */
    @SuppressWarnings("unchecked")
    public Map act(Redirector redirector,
            SourceResolver resolver,
            Map objectModel,
            String source, Parameters parameters)
    {
        try
        {
            String recipient = ConfigurationManager.getProperty("alert.recipient");
            if(recipient != null && recipient.trim().length() > 0)
            {
                Context context = ContextUtil.obtainContext(objectModel);
                
                // create email object using internal_error template
                Email email = ConfigurationManager.getEmail(
                        I18nUtil.getEmailFilename(context.getCurrentLocale(), "internal_error"));
                
                // build recipient list
                String recipients[] = recipient.split(",");
                for(String address : recipients)
                {
                    email.addRecipient(address.trim()); 
                }
                
                // 0 - instance URL
                String url = ConfigurationManager.getProperty("dspace.url");
                email.addArgument(url);

                // 1 - date
                email.addArgument(DATE_FORMAT.format(new Date()));

                // 2 - session id
                HttpServletRequest request =
                    (HttpServletRequest)objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
                email.addArgument(request.getSession().getId());

                // 3 - request URL
                email.addArgument(request.getRequestURI());

                // 4 - the exception callstacks
                StringBuffer buffer = new StringBuffer();
                Map exceptionDetails = ((Notifying)objectModel.get("notifying-object")).getExtraDescriptions();
                buffer.append("\nCause:\n");
                buffer.append(exceptionDetails.get("cause"));
                
                buffer.append("\n\nJava stacktrace:\n");
                buffer.append(exceptionDetails.get("stacktrace"));
                
                buffer.append("\nFull stacktrace:\n");
                buffer.append(exceptionDetails.get("full exception chain stacktrace"));
                email.addArgument(buffer.toString());
                
                // finally send the emails
                email.send();
            }
        }
        catch(Exception ex) // ensure any exceptions are caught and logged
        {
            LOG.warn("*** Attempted email exception failed: " + ex);
        }
        
        return null;
    }
}
