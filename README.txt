Jorum-DSpace README 
=========================

# Author GWaller
# Last Updated: 30th May 2011

About
=====

The Jorum project, which until August 2011, was jointly operated by EDINA and MIMAS (the two UK 
National Data Centres based at Universities of Edinburgh and Manchester), provides a free online 
repository of learning and teaching materials.

The repository software used was a significantly modified version of DSpace v1.5.2. 

Some of the modifications made by Jorum

    * Support for deposit of web links to resources (instead of binary files)
    * Learning package support - IMS and SCORM content package standards
    * Streamlined metadata profile
    * Metadata crosswalks - LOM, IMSMD, DC
    * Modified licence chooser
    * Virus checking
    * RSS deposit - RSS feeds parsed and items with metadata created in DSpace
    * Enhanced submitter privileges - allow the owner of a resource to add/edit metadata and bitstreams
    * Licence based authorization control
    * UI Enhancements e.g. JQuery community/collection browse tree, lightbox licence viewer, social bookmarking
    * Selenium tests
    * Item view pages contain view stats

See http://developer.edina.ac.uk/projects/jorum/wiki for more developer information on the Jorum project.

All technical development was carried out by the technical team based at EDINA (see AUTHORS.txt).

Installation
============

Please follow the instructions listed in the INSTALL.txt file to install the Jorum DSpace.

DSpace Web interface
====================

If no errors were received during installation, then the webapp should be running at: 

http://<jetty host>:<jetty port>/xmlui
e.g. 
http://localhost:8080/xmlui

To login browse to: http://<jetty host>:<jetty port>/xmlui/password-login 
e.g. 
http://localhost:8080/xmlui/password-login

NOTE: The "Depositor Login" link on the left hand side menu will *not* work, use the link above. 
This is because Jorum integrated with an internal authentication layer which is not supplied as part 
of this distribution.

OAI-PMH Web App
===============

The OAI-PMH web-app will be running at: 

http://<jetty host>:<jetty port>/oai/request
e.g. 
http://localhost:8080/oai/request

e.g. to list all the records with Dublin Core metadata: http://localhost:8080/oai/request?verb=ListRecords&metadataPrefix=oai_dc

For more information on OAI-PMH, please see http://www.openarchives.org/OAI/openarchivesprotocol.html 

SRW Web App
===========

The SRW web-app will be running at: 

http://<jetty host>:<jetty port>/srw
e.g. 
http://localhost:8080/srw

For more information on SRW, please see http://www.loc.gov/standards/sru/

SWORD Web App
=============

The SWORD web-app will be running at: 

http://<jetty host>:<jetty port>/sword
e.g. 
http://localhost:8080/sword

To service document will be located at http://<jetty host>:<jetty port>/sword/servicedocument

e.g. to obtain the service document using Curl and using the DSpace account "root@localhost" with password "dspace":

curl -v http://root%40localhost:dspace@localhost:8080/sword/servicedocument

For more information on SWORD, please see: http://swordapp.org/

Code Coverage
=============

Code coverage reports are very useful for testing to determine which lines of code have been executed. 
The Jorum build process supports code coverage via Cobertura (http://cobertura.sourceforge.net). 

To use code coverage follow the steps below:

1. Instrument the code by running: ant -DPROPS=etc/ubuntu_build.properties instrument_code deploy recycle_jetty
2. Perform any tests you wish using your web browser
3. Generate the code coverage report by running: ant -DPROPS=etc/ubuntu_build.properties do_coverage_report 

The code coverage report will be available in HTML and XML formats in the following directories:

./build/cobertura/report/html
./build/cobertura/report/xml

e.g. to view the HTML report, open the file:

./build/cobertura/report/html/index.html



