function J = computeCost(X, y, theta)
%COMPUTECOST Compute cost for linear regression
%   J = COMPUTECOST(X, y, theta) computes the cost of using theta as the
%   parameter for linear regression to fit the data points in X and y

% Initialize some useful values
m = length(y); % number of training examples

alpha = 0.01;

% You need to return the following variables correctly 
%J = 0;
J = 1/(2*m);
z=0;
dt=0;
for i=1:m
dt=( (theta(1)+theta(2)*X(i))-y(i) );
z=z+dt^2;

t1=theta(1)-((1/m)*alpha*dt);
t2=theta(2)-((1/m)*alpha*dt*X(i));
theta(1)=t1;
theta(2)=t2;
%for j=1:m

%t1=t1+dt*X(0);
%t1=t1+dt*X(0);


end

end
J=J*z;

% ====================== YOUR CODE HERE ======================
% Instructions: Compute the cost of a particular choice of theta
%               You should set J to the cost.





% =========================================================================

end
