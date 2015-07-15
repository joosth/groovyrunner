/**
 * GroovyRunner
 *
 * Copyright 2010-2015, Open-T B.V., and individual contributors as indicated
 * by the @author tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License
 * version 3 published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

/**
 * JavaScript module for GroovyRunner
 * initializese CodeMirror edit boxes
 * Posts script to backend and displays the result
 */

var groovyrunner={};
groovyrunner.sendit=function() {
    groovyrunner.editor.save();
    url=window.location.href;
    url=url.replace("/index.html","");
    var parameters=document.getElementById('parameters').value;
    url=url+'?'+parameters;

    var dataElement=document.getElementById('script');
    var data=dataElement.value;
    groovyrunner.resulteditor.setValue('Running ....');
    $("#runner").addClass("groovyrunner-busy");
        $.ajax(url,{
            method:"POST",
            data:data,
            processData:false,
            contentType:"text/plain",
            success:function(resultData) {
                groovyrunner.resulteditor.setValue(resultData);
                 $("#runner").removeClass("groovyrunner-busy");
            },

            error:function(jqXHR,textStatus,errorThrown) {
                groovyrunner.resulteditor.setValue("An error occurred: "+textStatus);
                 $("#runner").removeClass("groovyrunner-busy");
            }
        }
    );
    return false;
};

$(function() {

    // Intiialize script source area
    var scriptContainerWidth=$("#script-container").css("width");
    scriptContainerWidth=scriptContainerWidth.replace("px","");
    var textarea=$("#script")[0];
    groovyrunner.editor = CodeMirror.fromTextArea(textarea, {
        mode: 'text/x-groovy',
        lineNumbers: true,
        matchBrackets:true
    });
    groovyrunner.editor.setSize(parseInt(scriptContainerWidth),300);

    // Initialize result area
    var resultContainerWidth=$("#result-container").css("width");
    resultContainerWidth=resultContainerWidth.replace("px","");
    var resultarea=$("#result")[0];
    groovyrunner.resulteditor = CodeMirror.fromTextArea(resultarea, {
        lineNumbers: true
    });
    groovyrunner.resulteditor.setSize(parseInt(resultContainerWidth),300);

    $("#runbutton").on("click",groovyrunner.sendit);
    $(document).keypress(function(e) {
        if (e.charCode===114 && e.altKey===true) {
            groovyrunner.sendit();
        }
    });
});