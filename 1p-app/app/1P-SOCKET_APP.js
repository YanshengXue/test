(function() {
var onepApp = angular.module('1P-SOCKET_APP',[]);
onepApp.controller('oneAppCtlr',['$scope', '$http', function($scope, $http){
  
    $scope.eventName = "Notification";
    $scope.eventInterval = 1000;
    $scope.eventLog = "";
    $scope.sources = [];

    $scope.startEvents = function() {
      var url;
      url = 'ws://localhost:7003/';
      console.log("opening socket url:" + url);
      $scope.ws = new WebSocket(url);
      $scope.ws.onopen = function(){  
          console.log("Socket has been opened!");  
          
          $scope.ws.send("xxxxxx");
      };
      
      $scope.ws.onmessage = function(message) {
          console.log("Got data from websocket: " +  message );
      };
     
      
      
    };

    $scope.stopEvents = function() {
    	$scope.ws.close();
    }
    
    $scope.clearLog = function() {
      $scope.eventLog = "";
    }
}]);

})();