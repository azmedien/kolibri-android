module Fastlane
  module Actions
    module SharedValues
    end

    class KolibriAction < Action
      def self.run(params)
        require 'net/http'
        require 'uri'

        uri = URI.parse("#{params[:kolibri_url]}/webhooks/receive")
        https = Net::HTTP.new(uri.host, uri.port)
        https.use_ssl = true

        req = Net::HTTP::Post.new(uri.request_uri)

        req.set_form_data({
          "project" => params[:kolibri_project],
          "build"   => params[:build] || ENV['BUILD_ID'],
          "type"    => params[:stage],
          "status"  => params[:state]
        })

        response = https.request(req)
        check_response_code(response)
      end

      def self.check_response_code(response)
        case response.code.to_i
        when 200, 204
          true
        when 404
          UI.user_error!("Kolibri Cocpit not found")
        when 401
          UI.user_error!("Access denied to Kolibri Cockpit")
        else
          UI.user_error!("Unexpected #{response.code} from Kolibri Cockpit with response: #{response.body}")
        end
      end

      #####################################################
      # @!group Documentation
      #####################################################

      def self.description
        "A short description with <= 80 characters of what this action does"
      end

      def self.details
        # Optional:
        # this is your chance to provide a more detailed description of this action
        "You can use this action to do cool things..."
      end

      def self.available_options
        # Define all options your action supports.

        # Below a few examples
        [
            FastlaneCore::ConfigItem.new(key: :kolibri_url,
                                         env_name: "KOLIBRI_URL",
                                         description: "Create an Incoming WebHook for Kolibri Cockpit",
                                         verify_block: proc do |value|
                                            UI.user_error!("No URL was given, pass using `kolibri_url: 'url'`") unless (value and not value.empty?)
                                         end),
            FastlaneCore::ConfigItem.new(key: :kolibri_project,
                                         env_name: "KOLIBRI_PROJECT",
                                         description: "Set project unique id for the Kolibri Cockpit",
                                         verify_block: proc do |value|
                                            UI.user_error!("No project id was given, pass using `kolibri_project: 'id'`") unless (value and not value.empty?)
                                         end),
            FastlaneCore::ConfigItem.new(key: :stage,
                                         env_name: "KOLIBRI_STAGE",
                                         description: "Webhook project stage to be reported",
                                         is_string: true,
                                         optional: true),
            FastlaneCore::ConfigItem.new(key: :state,
                                         env_name: "KOLIBRI_STATE",
                                         description: "Webhook project state of the stage to be reported",
                                         is_string: true,
                                         optional: true),
            FastlaneCore::ConfigItem.new(key: :build,
                                         env_name: "KOLIBRI_BUILD",
                                         description: "Webhook project build id to be reported",
                                         is_string: false,
                                         optional: true),
        ]
      end

      def self.output
        # Define the shared values you are going to provide
        # Example
      end

      def self.return_value
        # If you method provides a return value, you can describe here what it does
      end

      def self.authors
        ["L3K0V"]
      end

      def self.is_supported?(platform)
        true
      end
    end
  end
end
