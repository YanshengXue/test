(function() {
var onepApp = angular.module('1P-APP',[]);
onepApp.controller('oneAppCtlr',['$scope', '$http', function($scope, $http){
    $scope.method = 'GET';
  //Change the Service URL as required - Going through Zuul requires additional config work.
    $scope.url = 'http://localhost:7001/hello';
    //$scope.url = '/api/hello';
    $scope.code = null;
    $scope.response = null;
    $http({method: $scope.method, url: $scope.url}).
        success(function(data, status) {
            $scope.status = status;
            $scope.data = data;
        }).
        error(function(data, status) {
            $scope.data = data || "Request failed";
            $scope.status = status;
        });

    $scope.eventName = "Notification";
    $scope.eventInterval = 1000;
    $scope.eventLog = "";
    $scope.sources = [];

    $scope.startEvents = function() {
      var url;
      if ($scope.eventName) {
        url = 'http://localhost:7001/sse?name=' + $scope.eventName + '&interval=' + $scope.eventInterval;
      } else {
        url = 'http://localhost:7001/sse?interval=' + $scope.eventInterval;
      }
      console.log("sse url:" + url);
      var source = new EventSource(url);
      $scope.sources.push(source);
      source.onopen = function (event) {
        console.log("sse opened:" + event);
      }
      if ($scope.eventName) {
        source.addEventListener($scope.eventName, function (event) {
          var data = eval("(" + event.data + ")")
          $scope.$apply(function () {
            console.log("type: " + event.type + " message: " + data.message);
            $scope.eventLog += event.type + ": " + data.message + "\n";
          });
        });
      } else {
        source.onmessage = function (event) {
          var data = eval("(" + event.data + ")")
          console.log("message: " + data.message);
          $scope.eventLog += data.message + "\n";
        }
      }
      source.onerror = function (event) {
        console.log("sse error:" + event);
      }
    };

    $scope.stopEvents = function() {
      console.log("stopping all event sources: " + $scope.sources.length);
      for (var i = 0; i < $scope.sources.length; i++) {
        $scope.sources[i].close();
      }
      $scope.sources = [];
    }

    $scope.clearLog = function() {
      $scope.eventLog = "";
    }
}]);

})();