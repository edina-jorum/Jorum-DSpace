/**
 * 
 */
package org.dspace.authenticate;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authenticate.LDAPAuthentication.SpeakerToLDAP;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * @author Ian Fieldhouse
 *
 */
public class EdiauthAuthentication implements AuthenticationMethod {

	private static final String JORUMUK_USER_SERVICE = "jorum";
    private static final String EA_SERVICES = "ea_services";
    private static final String JORUMUK_USER_GROUP = "JorumUKUser";
    
    /** log4j category */
    private static Logger log = Logger.getLogger(EdiauthAuthentication.class);
	
    /**
	 * This is an explicit method as it require 
	 * a shibbid from some source
	 * 
	 * @return false
	 */
	public boolean isImplicit() {
		return false;
	}
	
	/**
     * Returns message key for title of the "login" page, to use
     * in a menu showing the choice of multiple login methods.
     *
     * @param context
     *            DSpace context, will be modified (EPerson set) upon success.
     *
     * @return Message key to look up in i18n message catalog.
     */
    public String loginPageTitle(Context context)
    {
        return "org.dspace.eperson.EdiauthAuthentication.title";
    }

	/**
     * Returns URL of ediauth-login servlet.
     *
     * @param context
     *            DSpace context, will be modified (EPerson set) upon success.
     *
     * @param request
     *            The HTTP request that started this operation, or null if not applicable.
     *
     * @param response
     *            The HTTP response from the servlet method.
     *
     * @return fully-qualified URL
     */
	public String loginPageURL(Context context, 
			                   HttpServletRequest request,
			                   HttpServletResponse response) 
	{
		return response.encodeRedirectURL(request.getContextPath() + "/ediauth-login");
	}
    
    /**
	 * Allow new users to be registered automatically if the Ediauth 
	 * server provides sufficient info (and user not exists in DSpace)
	 * 
	 * @param context
     *            DSpace context
     * @param request
     *            HTTP request, in case anything in that is used to decide
     * @param email
     *            e-mail address of user attempting to register
     * @return Property from dspace.cfg file
	 */
	public boolean canSelfRegister(Context context, 
			                       HttpServletRequest request,
                                   String username) 
	    throws SQLException 
	{
		return ConfigurationManager.getBooleanProperty("authentication.ediauth.autoregister");
	}
    
    /**
     * Indicate whether or not a particular self-registering user can set
     * themselves a password in the profile info form.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request, in case anything in that is used to decide
     * @param email
     *            e-mail address of user attempting to register
     * @return Users cannot set their own passwords
     * 
     */
	public boolean allowSetPassword(Context context,
			                        HttpServletRequest request, 
			                        String email) 
	    throws SQLException 
	{
		return false;
	}
	
	/**
     * Add authenticated users to the group defined in dspace.cfg by
     * the ediauth.login.specialgroup key.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request, in case anything in that is used to decide 
     * @return Special group id
     */
    public int[] getSpecialGroups(Context context, 
    		                      HttpServletRequest request)
    {
        // Prevents non-ediauth users from being added to this group
		try
		{
			if (!context.getCurrentUser().getNetid().equals(""))
			{
				String groupName = ConfigurationManager.getProperty("ediauth.login.specialgroup");
				if ((groupName != null) && (!groupName.trim().equals("")))
				{
				    Group specialGroup = Group.findByName(context, groupName);
					if (specialGroup == null)
					{
						// Oops - the group isn't there.
						log.warn(LogManager.getHeader(context,
								"ediauth_specialgroup",
								"Group defined in password.ediauth.specialgroup does not exist"));
						return new int[0];
					} else
					{
						return new int[] { specialGroup.getID() };
					}
				}
			}
		}
		catch (Exception e) {
			// The user is not an ediauth user, so we don't need to worry about them
		}
		return new int[0];
    }

    /**
     * Check credentials: shibbid must match the netid of an EPerson record, 
     * and that EPerson must be allowed to login. Also checks for EPerson that 
     * is only allowed to login via an implicit method and returns <code>CERT_REQUIRED</code> 
     * if that is the case.
     *
     * @param context
     *  DSpace context, will be modified (EPerson set) upon success.
     *
     * @param shibbid
     *  Shibboleth ID when method is explicit. Use null for implicit method.
     *
     * @param password
     *  Null for both explicit and implicit methods.
     *
     * @param realm
     *  Realm is an extra parameter used by some authentication methods, leave null if
     *  not applicable.
     *
     * @param request
     *  The HTTP request that started this operation, or null if not applicable.
     *
     * @return One of:
     *   SUCCESS, CERT_REQUIRED, NO_SUCH_USER, BAD_ARGS
     * <p>Meaning:
     * <br>SUCCESS         - authenticated OK.
     * <br>CERT_REQUIRED   - not allowed to login this way without X.509 cert.
     * <br>NO_SUCH_USER    - no EPerson with matching email address.
     * <br>BAD_ARGS        - missing shibbid, or user matched but cannot login.
     */
	public int authenticate(Context context, 
			                String shibbid, 
			                String password,
			                String realm, 
			                HttpServletRequest request) 
	    throws SQLException 
	{
		log.info(LogManager.getHeader(context, "auth", "attempting authentication of user="+shibbid));

        // Return BAD_ARGS when no shibbid is supplied or when a password is present.
        if ((shibbid == null) || (password != null))
        	return BAD_ARGS;

        // Locate the eperson
        EPerson eperson = null;
        try
        {
        	eperson = EPerson.findByNetid(context, shibbid);
        }
        catch (SQLException e)
        {
        }
        boolean loggedIn = false;
        
        // if they entered a shibbid that matches an eperson
        if (eperson != null)
        {
            // shibbid corresponds to active account
            if (eperson.getRequireCertificate())
                return CERT_REQUIRED;
            else if (!eperson.canLogIn())
                return BAD_ARGS;
            
            log.info(LogManager.getHeader(context, "authenticate", "type=ediauth"));
            context.setCurrentUser(eperson);
            
            
            // Check if user should be added to the JorumUKUser group
            try{
                  context.setIgnoreAuthorization(true);
                  checkJorumUKUserGroup(context, shibbid, request, eperson);
            }
            catch (AuthorizeException e)
            {
                return NO_SUCH_USER;
            }
            finally
            {
                context.setIgnoreAuthorization(false);
            }
            
            return SUCCESS;
        }
        // the user does not already exist so create an eperson for them
        else
        {
            // Register the new user automatically
            log.info(LogManager.getHeader(context, "autoregister", "netid=" + shibbid));

            if (canSelfRegister(context, request, shibbid))
            {
            	try
            	{
            		context.setIgnoreAuthorization(true);
            		eperson = EPerson.create(context);
            		eperson.setEmail(shibbid);
            		// if ((ldap.ldapGivenName!=null)&&(!ldap.ldapGivenName.equals(""))) eperson.setFirstName(ldap.ldapGivenName);
            		// if ((ldap.ldapSurname!=null)&&(!ldap.ldapSurname.equals(""))) eperson.setLastName(ldap.ldapSurname);
            		eperson.setNetid(shibbid);
            		eperson.setCanLogIn(true);
            		AuthenticationManager.initEPerson(context, request, eperson);
            		eperson.update();
            		context.commit();
            		context.setCurrentUser(eperson);
            		
            		// Check if user should be added to JorumUKUser group
                    checkJorumUKUserGroup(context, shibbid, request, eperson);
            		
            	}
            	catch (AuthorizeException e)
            	{
            		return NO_SUCH_USER;
            	}
            	finally
            	{
            		context.setIgnoreAuthorization(false);
            	}

            	log.info(LogManager.getHeader(context, "authenticate", "type=ediauth-login, created ePerson"));
            	return SUCCESS;
            }
            else
            {
            	// No auto-registration for valid certs
            	log.info(LogManager.getHeader(context, "failed_login", "type=ediauth-login"));
            	return NO_SUCH_USER;
            }
        }
	}

	
	
    private void checkJorumUKUserGroup(Context context, String shibbid,
            HttpServletRequest request, EPerson eperson) throws SQLException,
            AuthorizeException
    {
        String ea_services = request.getParameter(EA_SERVICES);
        try
        {
            if (ea_services != null)
            {
                String decodedServices = URLDecoder.decode(ea_services, "UTF-8");
                log.info("Decoded ea_services: " + decodedServices);
                String[] services = decodedServices.split(",");
                for (String service : services)
                {
                    if (service.trim().equals(JORUMUK_USER_SERVICE))
                    {
                        // Check if already in JorumUKUser
                        Group jorumUKUserGroup = Group.findByName(context, JORUMUK_USER_GROUP);
                        if (!jorumUKUserGroup.isMember(eperson))
                        {
                            jorumUKUserGroup.addMember(eperson);
                            log.info("Added " + shibbid + " to JorumUKUser group");
                            jorumUKUserGroup.update();
                            break;
                        }
                    }

                }
            }
        }
        catch (UnsupportedEncodingException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
	
	
	/**
     *  Nothing extra to initialize.
     */
	public void initEPerson(Context context, 
			                HttpServletRequest request, 
			                EPerson eperson) 
	    throws SQLException 
	{
	}
}
