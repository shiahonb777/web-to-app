from django.urls import path
from mysite import views

urlpatterns = [
    path('', views.dashboard),
    path('api/stats', views.api_stats),
]
