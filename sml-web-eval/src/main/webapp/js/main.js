(function() {
	
	var tableModel = {
			name: 'Person',
			head: [{
				name: 'id',
				type: 'int'
			}, {
				name: 'name',
				type: 'text'
			}],
			body: [
				{id: 1, name: 'Claus'},
				{id: 2, name: 'Joerg'}
			]
		};

	var expected = {
			  "http://example.org/about" : 
			    {
			       "http://purl.org/dc/elements/1.1/title": [ { "type" : "literal" , "value" : "Anna's Homepage" }, { "type" : "literal" , "value" : "Peter's Homepage" } ]
			    },
			    
				  "http://example.org/goobar" : 
				    {
				       "http://purl.org/dc/elements/1.1/title": [ { "type" : "literal" , "value" : "Anna's Homepage" }, { "type" : "literal" , "value" : "Peter's Homepage" } ]
				    },
			};
		
	var actual = {
			  "http://example.org/about" : 
			    {
			       "http://purl.org/dc/elements/1.1/title": [ { "type" : "literal" , "value" : "Anna's Homepage" } ]
			    },
			  "http://example.org/foo" : 
			    {
			       "http://example.org/bar": [ { "type" : "literal" , "value" : "Baz" } ]
			    }
	};

	
	
	
	
		/**
		 * Adjusts the frontend according to the backend's state.
		 * Concretely loads the tasks and score sheet.
		 */
		var resetAppTest = function() {
	
			for(var i = 0; i < 4; ++i) {
				
				var taskNo = i + 1;
				
				utils.createTab('#tabs', 'task' + taskNo, 'Task ' + taskNo, 'Foobar', 1 + i);
				
			}
			
	    	var tableHtml = tables.renderTable(tableModel);
	    	$('.tableArea').html(tableHtml);
	    	
	    	
			var diff = rdfDiff.createDiff(expected, actual);
	
			var html = rdfDiff.renderDiff(diff);
			
			//console.log('HTML', html);
			var $elTarget = $('.rdfdiff');
			console.log('Target Element: ', $elTarget);
			$elTarget.html(html);
			
	
		}
		
		
		
		var initTasks = function(tsks) {
			for(var i = 0; i < tsks.length; ++i) {
				var task = tsks[i];
				
				var taskNo = i + 1;
				
				var tab = utils.createTab('#tabs', 'task' + taskNo, 'Task ' + taskNo, 'Foobar', 1 + i);

				var view = tasks.createTaskView(task);
				
				tab.body.append(view);
			}
		};
		
	
		var resetApp = function() {
			var apiUrl = "api/0.1/";
			
			$.ajax({
				url: apiUrl + "fetchTasks",
				dataType: 'json'
			}).done(function(json) {
				
				initTasks(json);
				
			}).fail(function(json) {
				alert("Failed to initialize. Most likely the server is down or it cannot be reached. Message: " + JSON.stringif(msg));
			});
		}
		
	
		$(document).ready(function() {
	
			// Twitter Bootstrap's way of enabling tabs
			$('#tabs > ul > li > a').click(function (e) {
		        e.preventDefault();
		        $(this).tab('show');
		    });
	    	$('#tabs a:first').tab('show');
	
	    	resetApp();
	    	
	    	
		});
	
})();