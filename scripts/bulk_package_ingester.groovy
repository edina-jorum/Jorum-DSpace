/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : bulk_package_ingester.groovy
 *  Author              : gwaller
 *  Approver            : Gareth Waller 
 * 
 *  Notes               :
 *
 *
 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 */
import java.util.regex.*
import java.util.concurrent.*

options = null
packagerArgs = null
numThreads = 1
pool = null

def log = { msg -> 
		 println "${Thread.currentThread().getName()}: ${msg}"
}


def parseArgs = {args ->
	def cli = new CliBuilder(usage:'bulk_package_ingester.groovy')
	cli.d(args:1, required:true, argName:'dir', 'Package directory')
	cli.i(args:1, required:true, argName:'dspace', 'DSpace installation directory')
	cli.a(args:1, required:true, argName:'arg', 'Packager aguments as a single quoted string')
	cli.t(args:1, required:true, argName:'num', 'Number of threads')
	cli.help('print this message')
	
	options = cli.parse(args)
	
	if (!options){
		System.exit(1)
	}
	
	if (options.help){
		cli.usage()
		log """e.g.: groovy bulk_package_ingester.groovy -i /opt/dspace_jorum -d /Users/gwaller/edina/workspace/jorum -a " -c 123456789/31 -e g.waller@ed.ac.uk -t IMS -o validate=false -o alterOwningCol=true -o failNoLicence=true" """
		
	}
	
	if (options.t){
		try{
			numThreads = Integer.parseInt(options.t)
		} catch (Exception e){
			println "Invalid number for number of threads, using default: ${numThreads}"
		}
	}
	
	
	log "Using DSpace dir: ${options.i}"
	log "Using Package dir: ${options.d}"
	log "Using packager args dir: ${options.a}"
	log "Number of threads: ${numThreads}"
	log ""
	
	packagerArgs = options.a.trim()
	
	if (!packagerArgs.startsWith("-")){
		log "Packager command line args does not start with a hyphen. Groovy seems to erase the first hyphen if it immediately follows the quote. Please enter a quote than a space"
		log "e.g."
		log """groovy bulk_package_ingester.groovy -i /opt/dspace_jorum -d /Users/gwaller/edina/workspace/jorum -a " -c 123456789/31 -e g.waller@ed.ac.uk -t IMS -o validate=false -o alterOwningCol=true -o failNoLicence=true" """
		System.exit(1)
	}
}


def processFile = { file ->
  log ""
  log "Examining ${file} ..."
  
  // Check for a previous success file, if so don't re-install the item
  def successFilePath = file.getAbsolutePath() + ".installedHandle"
  def successFile = new File(successFilePath);
  if (successFile.exists()){
  	log "Previous success file detected, ignoring package"
  } else {
  
	  try{
		// Test valid zip
		def zipFile = new java.util.zip.ZipFile(file)
	    
	    // If we got here then its a valid zip - close so we don't leak the descriptor
	    zipFile.close();
	
		// split the args on whitespace
		def argList = packagerArgs.split("\\s+").toList()
		
		// Need to build up command in a List as this will allow filenames to include a space
		def commandList = [options.i + "/bin/dsrun", "org.dspace.app.packager.Packager"]
		
		for (a in argList) { commandList << a }
		
		commandList << file.getAbsolutePath()
		
		// Debug - print the command we are goign to run
		log "Excuting command: ${commandList.join(' ')}"
		
		def proc = commandList.execute()
		proc.waitFor() 
		//println "return code: ${proc.exitValue()}"
		//println "stderr: ${proc.err.text}"
		//println "stdout: ${proc.in.text}"
		//println ""
		
		if (proc.exitValue() == 0){
			// Exit value ok, now see if we can extract the handle for the process stdout
			
			Pattern p = Pattern.compile("(?s).*Created and installed item, handle=(.*)\$");
			Matcher m = p.matcher(proc.in.text);
				
			if (m.matches()){
				log "Successfully installed item with handle: ${m.group(1)}"
				successFile.append(m.group(1))
			} else {
				log "Error: \n${proc.err.text}\n${proc.in.text}" 
			}
		
		} else {
			log "Proccess returned non-zero exit code: ${proc.exitValue()}"
			log "Process stderr: ${proc.err.text}" 
		}
	
	  } catch (Exception e){
		log "${file} is NOT a Zip, ignoring .... ${e.getMessage()}"
	  } finally {
		// Tidy up - close the streams
		try { proc.err.close() } catch (Exception e) {}
		try { proc.in.close() } catch (Exception e) {}
	  }
	}

}


submitJob = { c -> pool.submit(c as Callable) }

def mainLoop = {args->

	parseArgs(args)
	
	// Create thread pool
	pool = Executors.newFixedThreadPool(numThreads)
	
	
	def dir = new File(options.d)
	dir.eachFile{file->
		// Notice the magic groovy syntax below with the {} - seems to be redefining the closure. Got from example at http://groovy.codehaus.org/Concurrency+with+Groovy
		def job = submitJob{ processFile(file) }
	}

	// shutdown the pool - should block until all jobs are finished!
	pool.shutdown()

}



mainLoop(args)



  
 




 
