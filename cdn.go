package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"github.com/aliyun/alibaba-cloud-sdk-go/services/cdn"
	"os"
	"regexp"
	"strings"
)

type Config struct {
	AccessKeyId     string
	AccessKeySecret string
	RegionId        string
	TaskType        string
	FilePath        string
	ObjectType      string
	Area            string
}

func main() {
	if len(os.Args) < 9 {
		fmt.Println("Usage: go run main.go -i <AccessKey> -k <AccessKeySecret> -r <FilePath> -t <TaskType> [-o <ObjectType>] [-a <Area>]")
		return
	}

	config := parseArgs(os.Args)
	client, err := cdn.NewClientWithAccessKey(config.RegionId, config.AccessKeyId, config.AccessKeySecret)
	if err != nil {
		fmt.Println("Error creating Alibaba Cloud client:", err)
		return
	}

	urls, err := readUrls(config.FilePath)
	if err != nil {
		fmt.Println("Error reading URLs from file:", err)
		return
	}

	switch config.TaskType {
	case "clear":
		err = refreshUrls(client, urls, config.ObjectType)
	case "push":
		err = preheatUrls(client, urls, config.Area)
	default:
		fmt.Println("Invalid task type. Use 'clear' or 'push'.")
		return
	}

	if err != nil {
		fmt.Println("Error executing task:", err)
	}
}

func parseArgs(args []string) *Config {
	config := &Config{}
	for i := 1; i < len(args); i++ {
		switch args[i] {
		case "-i":
			i++
			config.AccessKeyId = args[i]
		case "-k":
			i++
			config.AccessKeySecret = args[i]
		case "-r":
			i++
			config.FilePath = args[i]
		case "-t":
			i++
			config.TaskType = args[i]
		case "-o":
			i++
			config.ObjectType = args[i]
		case "-a":
			i++
			config.Area = args[i]
		}
	}
	config.RegionId = "cn-hangzhou" // Set your desired region
	return config
}

func readUrls(filePath string) ([]string, error) {
	file, err := os.Open(filePath)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	var urls []string
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		url := scanner.Text()
		if isValidUrl(url) {
			urls = append(urls, url)
		} else {
			fmt.Printf("Invalid URL format: %s\n", url)
		}
	}

	if err := scanner.Err(); err != nil {
		return nil, err
	}

	return urls, nil
}

func isValidUrl(url string) bool {
	re := regexp.MustCompile(`^https?://`)
	return re.MatchString(url)
}

func refreshUrls(client *cdn.Client, urls []string, objectType string) error {
	request := cdn.CreateRefreshObjectCachesRequest()
	request.Scheme = "https"
	request.ObjectPath = strings.Join(urls, "\n")
	request.ObjectType = objectType

	response, err := client.RefreshObjectCaches(request)
	if err != nil {
		return err
	}

	printResponse(response.GetHttpStatus(), response.GetHttpContentString())
	return nil
}

func preheatUrls(client *cdn.Client, urls []string, area string) error {
	request := cdn.CreatePushObjectCacheRequest()
	request.Scheme = "https"
	request.ObjectPath = strings.Join(urls, "\n")
	request.Area = area

	response, err := client.PushObjectCache(request)
	if err != nil {
		return err
	}

	printResponse(response.GetHttpStatus(), response.GetHttpContentString())
	return nil
}

func printResponse(status int, response string) {
	fmt.Printf("HTTP Status: %d\n", status)
	var prettyJSON strings.Builder
	if err := json.Indent(&prettyJSON, []byte(response), "", "    "); err != nil {
		fmt.Println("Failed to format JSON:", err)
		return
	}
	fmt.Println(prettyJSON.String())
}
