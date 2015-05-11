(function() {
var onepApp = angular.module('1P-SOCKET_APP',[]);
onepApp.controller('oneAppCtlr',['$scope', '$http', function($scope, $http){
  
    $scope.eventName = "Notification";
    $scope.eventInterval = 1000;
    $scope.eventLog = "";
    $scope.lastId = "-1";
    
    $scope.startEvents = function() {
      var url;
      url = 'ws://localhost:7003/';
      console.log("opening socket url:" + url);
      $scope.ws = new WebSocket(url);
      
      $scope.ws.onopen = function(){  
          console.log("Socket has been opened!");  
          $scope.ws.send($scope.lastId );
      };
    
      $scope.ws.onmessage = function(message) {
          console.log("Got data from websocket: " +  message.data );
          $scope.lastId = message.data;
          // $scope.ws.send("Resieved " + message);
      };
     
      $scope.ws.onerror = function(evt) {
    	 console.log("Socket error!: " + evt.data);  
    	 //$scope.ws = new WebSocket(url);
    	    
      };
    
      $scope.ws.onclose = function(evt) {
     	 console.log("Socket is Closed!: ");  
      };
     
    };

    $scope.stopEvents = function() {
    	$scope.ws.close();
    }
    
    $scope.clearLog = function() {
      $scope.eventLog = "";
    };
}]);

})();