function [X_norm, mu, sigma] = featureNormalize(X)
%FEATURENORMALIZE Normalizes the features in X 
%   FEATURENORMALIZE(X) returns a normalized version of X where
%   the mean value of each feature is 0 and the standard deviation
%   is 1. This is often a good preprocessing step to do when
%   working with learning algorithms.

% You need to set these values correctly
X_norm = X;
mu = zeros(1, size(X, 2)); % zeros(1, columns(X))
sigma = zeros(1, size(X, 2)); % zeros(1, columns(X))

% ====================== YOUR CODE HERE ======================
% Instructions: First, for each feature dimension, compute the mean
%               of the feature and subtract it from the dataset,
%               storing the mean value in mu. Next, compute the 
%               standard deviation of each feature and divide
%               each feature by it's standard deviation, storing
%               the standard deviation in sigma. 
%
%               Note that X is a matrix where each column is a 
%               feature and each row is an example. You need 
%               to perform the normalization separately for 
%               each feature. 
%
% Hint: You might find the 'mean' and 'std' functions useful.
%       

mu = mean(X);
sigma = std(X);


for i = 2:columns(X),
	X_cur = X(:,i);
	cur_std = sigma(1,i);
	X_mean = mu(1,i);
	
	X_cur2 = (X_cur .- X_mean) ./ cur_std;
	X_norm(:,i) = X_cur2;
	
	% if cur_std ~= 0
	

	 
	 % X_mean = mean(X_cur);
	 
	 % printf("col %i std is %.6f, mean is %.6f\n", i, cur_std, X_mean);
	 
	 
	 % X_cur2 = (X_cur .- X_mean) ./ cur_std;
	 % X_norm(:,i) = X_cur2;
 end 
 
end


% Your task here is to complete the code in featureNormalize.m to
% ? Subtract the mean value of each feature from the dataset.
% ? After subtracting the mean, additionally scale (divide) the feature values
% by their respective \standard deviations."
% 1

% Implementation Note: When normalizing the features, it is important
% to store the values used for normalization - the mean value and the stan-
% dard deviation used for the computations. After learning the parameters
% from the model, we often want to predict the prices of houses we have not
% seen before. Given a new x value (living room area and number of bed-
% rooms), we must rst normalize x using the mean and standard deviation
% that we had previously computed from the training set.


% ============================================================

end
