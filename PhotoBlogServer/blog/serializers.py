from blog.models import Post
from rest_framework import serializers
from django.contrib.auth.models import User

class PostSerializer (serializers.ModelSerializer) :
    author = serializers.PrimaryKeyRelatedField(queryset = User.objects.all())
    
    class Meta :
        model = Post
        
        #blog/models.py의 속성과 동일하게
        fields = ("author", "title", "text", "created_date", "published_date", "image")
