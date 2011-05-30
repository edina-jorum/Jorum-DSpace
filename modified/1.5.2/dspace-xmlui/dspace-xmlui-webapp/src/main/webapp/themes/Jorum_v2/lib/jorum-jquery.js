/*

	jorum-jquery.js
	A place for jorum jQuery functions

*/


	$(document).ready(function(){
		// All links in Community treeview go through this function
		// This ensures that clicking on a node that is a link doesn't 
		// expand the tree - instead it follows the link
		$('#tree a').click(function(){
		    // follow the link in the href attribute
			location.href=this.href;
			// have to return false to prevent event bubbling - otherwise the tree would expand 
			// briefly before the href target page opens up
		    return false;      
		}); 
		
		// all trees set to be collapsed by default
		$("#tree").treeview({
			animated: "medium",
			collapsed: true
			
		});
		
		/*
		 * Inject file list show/hide links into page and assign
		 * click events to each link so as to hide and display
		 * the file list while toggling the show/hide links.
		 * 
		 * For pages containing archived content packages
		 * hide the file list by default, otherwise show the 
		 * file list by default.
		 */
		
		// Inject show/hide links
		$("#package-files").before('<a href="#" id="toggle-files-show">Show file list</a>');
		$("#package-files").before('<a href="#" id="toggle-files-hide">Hide file list</a>');
		
		// Check if content package preview exists
		if ($("#cp-preview").length){
			$("#package-files").hide();
			$("#toggle-files-hide").hide();
		} else {
			$("#toggle-files-show").hide();
		}
		
		// Handle 'Show file list' clicked
		$("#toggle-files-show").click(function(){
			$("#toggle-files-show").hide();
			$("#toggle-files-hide").show();
			$("#package-files").show("slow");
			return false;
		});
		
		// Handle 'Hide file list' clicked
		$("#toggle-files-hide").click(function(){
			$("#toggle-files-hide").hide();
			$("#toggle-files-show").show();
			$("#package-files").hide("slow");
			return false;
		});
 			
		// Similar to above - show/hide for advanced search
		$("#aspect_artifactbrowser_AdvancedSearch_table_search-query").before('<a id="toggle-advanced-show" class="advanced">&#43; Show advanced search filter options</a>');
		$("#aspect_artifactbrowser_AdvancedSearch_table_search-query").before('<a id="toggle-advanced-hide" class="advanced">&#45; Hide advanced search filter options</a>');
		
		var $query1 =  $("input[name='query1']").val();
		var $query2 =  $("input[name='query2']").val();
		var $query3 =  $("input[name='query3']").val();
	 	
		
		if ($query1.length || $query2.length || $query3.length){
			$("#aspect_artifactbrowser_AdvancedSearch_table_search-query").hide();
			$("#toggle-advanced-hide").hide();
		} else {
			$("#toggle-advanced-show").hide();
		}
		
	
		$("#toggle-advanced-hide").click(function(){
			$("#toggle-advanced-hide").hide();
			$("#toggle-advanced-show").show();
			$("#aspect_artifactbrowser_AdvancedSearch_table_search-query").hide("slow");
			return false;
		});
		
		$("#toggle-advanced-show").click(function(){
			$("#toggle-advanced-show").hide();
			$("#toggle-advanced-hide").show();
			$("#aspect_artifactbrowser_AdvancedSearch_table_search-query").show("slow");
			return false;
		});
		
		
		// If the present path contains 'submit' we dynamically remove the underline from
		// 'Find' and add it to 'Share' in the menu bar
		if($.inArray("submit", window.location.pathname.split("/"))>-1){
			$("ul li:nth-child(3) a").addClass("_active");
			$("ul li:nth-child(2) a").removeClass("_active");
		}
	});
	

	$(function() {
		// Call the loading plugin whenever a form submit happens
		// This will place a div containing a spinner in the
		// top right of the first h1 element on the page
		$('form').submit(function() { 
			$('#second_level_content h1:first').loading({align: 'top-right'});
		}); 
		   
	});
