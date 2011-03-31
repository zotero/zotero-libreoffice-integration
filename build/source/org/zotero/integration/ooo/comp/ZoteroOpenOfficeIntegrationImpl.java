package org.zotero.integration.ooo.comp;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.uno.XComponentContext;


public final class ZoteroOpenOfficeIntegrationImpl extends WeakBase
   implements com.sun.star.task.XJobExecutor,
              com.sun.star.lang.XServiceInfo
{
    private static Application mApplication;
    private static Comm mComm;
    private static final String m_implementationName = ZoteroOpenOfficeIntegrationImpl.class.getName();
    private static final String[] m_serviceNames = {
        "org.zotero.integration.ooo.ZoteroOpenOfficeIntegration" };
    private static final String[] WINDOW_NAMES = { "ZoteroMessageWindow", "FirefoxMessageWindow",
    	"MinefieldMessageWindow", "BrowserMessageWindow" };
    private static final String[] ACTIVATE_FOR_COMMANDS = { "addCitation", "editCitation",
    	"editBibliography", "setDocPrefs" };
    
    public interface CLibrary extends Library {
    	CLibrary INSTANCE = (Platform.isWindows() ?
    			(CLibrary) Native.loadLibrary("user32", CLibrary.class)
    			: null);
    	
    	int FindWindowA(String lpClassName, String lpWindowName);
    	boolean SetForegroundWindow(int hWnd);
    }
    
    public static void debugPrint(String msg) {
    	System.out.println("ZoteroOpenOfficeIntegration: "+msg);
    }
    
    public ZoteroOpenOfficeIntegrationImpl( XComponentContext context ) throws Exception
    {
        mApplication = new Application(context);
        debugPrint("Service initialized");
    };

    public static XSingleComponentFactory __getComponentFactory( String sImplementationName ) {
        XSingleComponentFactory xFactory = null;

        if ( sImplementationName.equals( m_implementationName ) )
            xFactory = Factory.createComponentFactory(ZoteroOpenOfficeIntegrationImpl.class, m_serviceNames);
        return xFactory;
    }

    public static boolean __writeRegistryServiceInfo( XRegistryKey xRegistryKey ) {
        return Factory.writeRegistryServiceInfo(m_implementationName,
                                                m_serviceNames,
                                                xRegistryKey);
    }

    // com.sun.star.lang.XServiceInfo:
    public String getImplementationName() {
         return m_implementationName;
    }

    public boolean supportsService( String sService ) {
        int len = m_serviceNames.length;

        for( int i=0; i < len; i++) {
            if (sService.equals(m_serviceNames[i]))
                return true;
        }
        return false;
    }

    public String[] getSupportedServiceNames() {
        return m_serviceNames;
    }
    
	public void trigger(String command) {
		if(Platform.isWindows()) {
			boolean activateWindow = false;
			for(String cmd : ACTIVATE_FOR_COMMANDS) {
				if(command.equals(cmd)) {
					activateWindow = true;
					break;
				}
			}
			
			if(activateWindow) {
				debugPrint("Activating window");
				// Look for Firefox/Zotero window
				int hWnd = 0;
				for(String window : WINDOW_NAMES) {
					hWnd = CLibrary.INSTANCE.FindWindowA(window, null);
					if(hWnd != 0) break;
				}
				
				// Activate window
				if(hWnd != 0) {
					CLibrary.INSTANCE.SetForegroundWindow(hWnd);
				}
			}
		}
		
		debugPrint("Executing "+command);
		try {
			if(mComm == null) mComm = new Comm(mApplication);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		try {
			mComm.sendCommand(command);
		} catch(Exception e) {
			mComm.showError(Comm.COMMUNICATION_ERROR_STRING, e);
		}
	}
}
